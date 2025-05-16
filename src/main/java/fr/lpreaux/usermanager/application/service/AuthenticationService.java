package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.InvalidCredentialsException;
import fr.lpreaux.usermanager.application.exception.InvalidTokenException;
import fr.lpreaux.usermanager.application.port.in.AuthenticationUseCase;
import fr.lpreaux.usermanager.application.port.in.UserRoleUseCase;
import fr.lpreaux.usermanager.application.port.out.JwtTokenProvider;
import fr.lpreaux.usermanager.application.port.out.SecurityAuditLogger;
import fr.lpreaux.usermanager.application.port.out.TokenBlacklistRepository;
import fr.lpreaux.usermanager.application.port.out.UserRepository;
import fr.lpreaux.usermanager.domain.model.Role;
import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.Login;
import fr.lpreaux.usermanager.infrastructure.monitorig.SecurityMetrics;
import fr.lpreaux.usermanager.infrastructure.persistence.adapter.RedisTokenBlacklistAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service d'authentification des utilisateurs.
 * Gère l'authentification, la validation et le rafraîchissement des tokens JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationService implements AuthenticationUseCase {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRoleUseCase userRoleUseCase;
    private final SecurityAuditLogger securityAuditLogger;
    private final SecurityMetrics securityMetrics;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    // Compteur d'échecs d'authentification par utilisateur (en mémoire pour l'exemple)
    private final Map<String, AtomicInteger> failedLoginAttempts = new HashMap<>();

    // Seuil de tentatives échouées avant verrouillage temporaire
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public AuthenticationResultDTO authenticate(AuthenticateCommand command) {
        log.info("Authenticating user with login: {}", command.login());

        String clientInfo = "Web client"; // À remplacer par l'info réelle du client

        try {
            // Vérifier si l'utilisateur n'est pas verrouillé
            if (isUserLocked(command.login())) {
                log.warn("Authentication blocked: Account temporarily locked for user: {}", command.login());
                securityAuditLogger.logSecurityEvent(
                        "login_blocked",
                        "unknown",
                        clientInfo,
                        false,
                        Map.of("login", command.login(), "reason", "account_locked")
                );
                throw new InvalidCredentialsException("Account is temporarily locked due to multiple failed attempts");
            }

            User user = userRepository.findByLogin(Login.of(command.login()))
                    .orElseThrow(() -> {
                        log.warn("Authentication failed: User not found with login: {}", command.login());
                        recordFailedLogin(command.login());
                        securityAuditLogger.logSecurityEvent(
                                "login_failed",
                                "unknown",
                                clientInfo,
                                false,
                                Map.of("login", command.login(), "reason", "user_not_found")
                        );
                        return new InvalidCredentialsException("Invalid credentials");
                    });

            if (!user.getPassword().matches(command.password())) {
                log.warn("Authentication failed: Invalid password for user: {}", command.login());
                recordFailedLogin(command.login());
                securityAuditLogger.logSecurityEvent(
                        "login_failed",
                        user.getId().getValue().toString(),
                        clientInfo,
                        false,
                        Map.of("login", command.login(), "reason", "invalid_password")
                );
                securityMetrics.incrementLoginFailure();
                throw new InvalidCredentialsException("Invalid credentials");
            }

            // Réinitialiser le compteur d'échecs en cas de succès
            resetFailedLoginCounter(command.login());

            // Récupérer les rôles et permissions de l'utilisateur
            Set<String> roles = extractRoleNames(user);
            Set<String> permissions = userRoleUseCase.getUserPermissions(user.getId().getValue().toString());

            // Métadonnées supplémentaires pour le token
            Map<String, Object> additionalClaims = new HashMap<>();
            additionalClaims.put("client_info", clientInfo);
            additionalClaims.put("auth_time", System.currentTimeMillis());

            // Générer le token JWT
            String token = jwtTokenProvider.generateToken(
                    user.getId().getValue().toString(),
                    user.getLogin().getValue(),
                    roles,
                    permissions,
                    additionalClaims
            );

            JwtTokenProvider.JwtTokenInfo tokenInfo = jwtTokenProvider.validateToken(token);

            // Si l'implémentation de TokenBlacklistRepository est RedisTokenBlacklistAdapter,
            // on peut enregistrer le token pour la gestion des sessions
            if (tokenBlacklistRepository instanceof RedisTokenBlacklistAdapter redisAdapter) {
                redisAdapter.registerUserToken(user.getId().getValue().toString(), token, clientInfo);
            }

            // Enregistrer l'événement d'authentification réussie
            securityAuditLogger.logSecurityEvent(
                    "login_success",
                    user.getId().getValue().toString(),
                    clientInfo,
                    true,
                    Map.of(
                            "login", user.getLogin().getValue(),
                            "roles", roles.toString(),
                            "token_exp", tokenInfo.expiresAt()
                    )
            );

            securityMetrics.incrementLoginSuccess();
            log.info("User authenticated successfully: {}", user.getLogin().getValue());

            return new AuthenticationResultDTO(
                    user.getId().getValue().toString(),
                    user.getLogin().getValue(),
                    token,
                    roles,
                    permissions,
                    tokenInfo.expiresAt()
            );
        } catch (InvalidCredentialsException e) {
            // Déjà géré plus haut
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication", e);
            securityAuditLogger.logSecurityEvent(
                    "login_error",
                    "unknown",
                    clientInfo,
                    false,
                    Map.of("login", command.login(), "error", e.getMessage())
            );
            securityMetrics.incrementLoginFailure();
            throw new InvalidCredentialsException("Authentication failed");
        }
    }

    @Override
    public AuthenticationResultDTO validateToken(String token) {
        try {
            log.debug("Validating JWT token");

            // Vérifier si le token est dans la liste noire
            if (tokenBlacklistRepository.isBlacklisted(token)) {
                log.warn("Token validation failed: Token is blacklisted");
                throw new InvalidTokenException("Token has been revoked");
            }

            JwtTokenProvider.JwtTokenInfo tokenInfo = jwtTokenProvider.validateToken(token);
            securityMetrics.incrementTokenValidation();

            return new AuthenticationResultDTO(
                    tokenInfo.userId(),
                    tokenInfo.login(),
                    token,
                    tokenInfo.roles(),
                    tokenInfo.permissions(),
                    tokenInfo.expiresAt()
            );
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    @Override
    public AuthenticationResultDTO refreshToken(String token) {
        log.debug("Refreshing JWT token");

        try {
            // Vérifier si le token est dans la liste noire
            if (tokenBlacklistRepository.isBlacklisted(token)) {
                log.warn("Token refresh failed: Token is blacklisted");
                throw new InvalidTokenException("Token has been revoked");
            }

            JwtTokenProvider.JwtTokenInfo oldTokenInfo = jwtTokenProvider.validateToken(token);
            String newToken = jwtTokenProvider.refreshToken(token);
            JwtTokenProvider.JwtTokenInfo newTokenInfo = jwtTokenProvider.validateToken(newToken);

            securityMetrics.incrementTokenRefresh();

            // Journaliser l'événement de rafraîchissement
            securityAuditLogger.logSecurityEvent(
                    "token_refresh",
                    oldTokenInfo.userId(),
                    "token_refresh",
                    true,
                    Map.of(
                            "old_token_exp", oldTokenInfo.expiresAt(),
                            "new_token_exp", newTokenInfo.expiresAt()
                    )
            );

            return new AuthenticationResultDTO(
                    newTokenInfo.userId(),
                    newTokenInfo.login(),
                    newToken,
                    newTokenInfo.roles(),
                    newTokenInfo.permissions(),
                    newTokenInfo.expiresAt()
            );
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    // Méthodes privées

    private Set<String> extractRoleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    private void recordFailedLogin(String login) {
        failedLoginAttempts.computeIfAbsent(login, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    private void resetFailedLoginCounter(String login) {
        failedLoginAttempts.remove(login);
    }

    private boolean isUserLocked(String login) {
        AtomicInteger counter = failedLoginAttempts.get(login);
        return counter != null && counter.get() >= MAX_FAILED_ATTEMPTS;
    }
}