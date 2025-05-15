package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record AuthenticationResponse(
        @Schema(description = "Token JWT d'authentification")
        String token,

        @Schema(description = "ID de l'utilisateur authentifié")
        String userId,

        @Schema(description = "Login de l'utilisateur")
        String login,

        @Schema(description = "Rôles de l'utilisateur")
        Set<String> roles,

        @Schema(description = "Permissions de l'utilisateur")
        Set<String> permissions,

        @Schema(description = "Date d'expiration du token (timestamp Unix)")
        long expiresAt
) {}