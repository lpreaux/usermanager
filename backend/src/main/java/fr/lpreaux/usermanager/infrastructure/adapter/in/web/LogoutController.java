package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import fr.lpreaux.usermanager.application.port.in.LogoutUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour la gestion des déconnexions.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API d'authentification")
public class LogoutController {

    private final LogoutUseCase logoutUseCase;

    @PostMapping("/logout")
    @Operation(
            summary = "Déconnexion",
            description = "Déconnecte l'utilisateur en révoquant son token JWT"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Déconnexion réussie"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal String userId,
            HttpServletRequest request) {

        log.info("Logout request received for user: {}", userId);

        // Extraire le token du header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Extraire des informations sur l'appareil pour l'audit
            String userAgent = request.getHeader("User-Agent");
            String ipAddress = getClientIp(request);
            String deviceInfo = String.format("%s - %s", ipAddress, userAgent);

            // Exécuter la déconnexion
            logoutUseCase.logout(new LogoutUseCase.LogoutCommand(
                    token,
                    userId,
                    deviceInfo
            ));

            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/logout-all")
    @Operation(
            summary = "Déconnexion de toutes les sessions",
            description = "Déconnecte l'utilisateur de toutes ses sessions actives"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Déconnexion réussie"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal String userId) {
        logoutUseCase.logoutFromAllSessions(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-others")
    @Operation(
            summary = "Déconnexion des autres appareils",
            description = "Déconnecte l'utilisateur de tous les appareils sauf l'actuel"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Déconnexion réussie"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logoutOthers(
            @AuthenticationPrincipal String userId,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logoutUseCase.logoutFromOtherDevices(userId, token);
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * Utilitaire pour extraire l'adresse IP du client, en tenant compte des proxys.
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}