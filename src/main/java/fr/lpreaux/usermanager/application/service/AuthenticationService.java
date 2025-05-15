package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.InvalidCredentialsException;
import fr.lpreaux.usermanager.application.exception.InvalidTokenException;
import fr.lpreaux.usermanager.application.port.in.AuthenticationUseCase;
import fr.lpreaux.usermanager.application.port.in.UserRoleUseCase;
import fr.lpreaux.usermanager.application.port.out.JwtTokenProvider;
import fr.lpreaux.usermanager.application.port.out.UserRepository;
import fr.lpreaux.usermanager.domain.model.Role;
import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.Login;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Set;

/**
 * Service d'authentification des utilisateurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationService implements AuthenticationUseCase {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRoleUseCase userRoleUseCase;

    @Override
    public AuthenticationResultDTO authenticate(AuthenticateCommand command) {
        log.info("Authenticating user with login: {}", command.login());

        User user = userRepository.findByLogin(Login.of(command.login()))
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User not found with login: {}", command.login());
                    return new InvalidCredentialsException("Invalid credentials");
                });

        if (!user.getPassword().matches(command.password())) {
            log.warn("Authentication failed: Invalid password for user: {}", command.login());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Récupérer les rôles et permissions de l'utilisateur
        Set<String> roles = extractRoleNames(user);
        Set<String> permissions = userRoleUseCase.getUserPermissions(user.getId().getValue().toString());

        // Générer le token JWT
        String token = jwtTokenProvider.generateToken(
                user.getId().getValue().toString(),
                user.getLogin().getValue(),
                roles,
                permissions,
                new HashMap<>()
        );

        JwtTokenProvider.JwtTokenInfo tokenInfo = jwtTokenProvider.validateToken(token);

        log.info("User authenticated successfully: {}", user.getLogin().getValue());

        return new AuthenticationResultDTO(
                user.getId().getValue().toString(),
                user.getLogin().getValue(),
                token,
                roles,
                permissions,
                tokenInfo.expiresAt()
        );
    }

    @Override
    public AuthenticationResultDTO validateToken(String token) {
        try {
            log.debug("Validating JWT token");
            JwtTokenProvider.JwtTokenInfo tokenInfo = jwtTokenProvider.validateToken(token);

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
            JwtTokenProvider.JwtTokenInfo oldTokenInfo = jwtTokenProvider.validateToken(token);
            String newToken = jwtTokenProvider.refreshToken(token);
            JwtTokenProvider.JwtTokenInfo newTokenInfo = jwtTokenProvider.validateToken(newToken);

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

    private Set<String> extractRoleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());
    }
}