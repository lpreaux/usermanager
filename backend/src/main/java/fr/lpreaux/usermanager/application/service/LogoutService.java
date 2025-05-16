package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.InvalidTokenException;
import fr.lpreaux.usermanager.application.port.in.LogoutUseCase;
import fr.lpreaux.usermanager.application.port.out.JwtTokenProvider;
import fr.lpreaux.usermanager.application.port.out.SecurityAuditLogger;
import fr.lpreaux.usermanager.application.port.out.TokenBlacklistRepository;
import fr.lpreaux.usermanager.infrastructure.monitorig.SecurityMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service pour gérer la déconnexion des utilisateurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutService implements LogoutUseCase {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityAuditLogger securityAuditLogger;
    private final SecurityMetrics securityMetrics;

    @Override
    public void logout(LogoutCommand command) {
        try {
            log.info("Processing logout for user: {}", command.userId());

            // Extraire les informations du token
            JwtTokenProvider.JwtTokenInfo tokenInfo = jwtTokenProvider.validateToken(command.token());

            // Vérifier que le token appartient bien à l'utilisateur
            if (!tokenInfo.userId().equals(command.userId())) {
                log.warn("Logout attempt with token belonging to a different user");
                throw new InvalidTokenException("Token does not belong to the specified user");
            }

            // Ajouter le token à la liste noire
            tokenBlacklistRepository.addToBlacklist(command.token(), tokenInfo.expiresAt());

            // Journaliser l'événement
            securityAuditLogger.logSecurityEvent(
                    "logout",
                    command.userId(),
                    command.deviceInfo(),
                    true,
                    Map.of("token_expiry", tokenInfo.expiresAt())
            );

            // Incrémenter les métriques
            securityMetrics.incrementLogout();

            log.info("User successfully logged out: {}", command.userId());

        } catch (InvalidTokenException e) {
            log.warn("Failed to logout: Invalid token, {}", e.getMessage());

            // Journaliser l'échec
            securityAuditLogger.logSecurityEvent(
                    "logout_failed",
                    command.userId(),
                    command.deviceInfo(),
                    false,
                    Map.of("reason", "invalid_token")
            );

            // Remonter l'exception
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during logout", e);

            securityAuditLogger.logSecurityEvent(
                    "logout_failed",
                    command.userId(),
                    command.deviceInfo(),
                    false,
                    Map.of("reason", "unexpected_error", "error", e.getMessage())
            );

            throw new RuntimeException("Failed to process logout", e);
        }
    }

    @Override
    public void logoutFromAllSessions(String userId) {
        log.info("Logging out user from all sessions: {}", userId);

        // Blacklister tous les tokens de l'utilisateur
        tokenBlacklistRepository.blacklistAllUserTokens(userId, "user_initiated_logout_all");

        // Journaliser l'événement
        securityAuditLogger.logSecurityEvent(
                "logout_all_sessions",
                userId,
                "all_devices",
                true,
                Map.of()
        );

        log.info("User logged out from all sessions: {}", userId);
    }

    @Override
    public void logoutFromOtherDevices(String userId, String currentToken) {
        log.info("Logging out user from other devices: {}", userId);

        try {
            // Valider le token actuel
            JwtTokenProvider.JwtTokenInfo tokenInfo = jwtTokenProvider.validateToken(currentToken);

            if (!tokenInfo.userId().equals(userId)) {
                log.warn("Logout attempt with token belonging to a different user");
                throw new InvalidTokenException("Token does not belong to the specified user");
            }

            // Ici, idéalement, vous auriez un mécanisme pour blacklister tous les tokens
            // d'un utilisateur SAUF le token actuel.
            // Dans une implémentation complète, vous pourriez:
            // 1. Stocker un identifiant de device/session dans chaque token
            // 2. Maintenir une liste des sessions actives par utilisateur
            // 3. Blacklister toutes les sessions sauf celle actuelle

            // Pour illustration, nous simulons cette fonctionnalité avec un log
            log.info("In a complete implementation, all tokens except the current would be blacklisted");

            // Journaliser l'événement
            securityAuditLogger.logSecurityEvent(
                    "logout_other_devices",
                    userId,
                    "all_except_current",
                    true,
                    Map.of("current_token_expiry", tokenInfo.expiresAt())
            );

        } catch (Exception e) {
            log.error("Error during logout from other devices", e);

            securityAuditLogger.logSecurityEvent(
                    "logout_other_devices_failed",
                    userId,
                    "all_except_current",
                    false,
                    Map.of("reason", e.getMessage())
            );

            throw new RuntimeException("Failed to logout from other devices", e);
        }
    }
}