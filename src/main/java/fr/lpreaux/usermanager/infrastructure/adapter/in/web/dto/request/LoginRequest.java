package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Login is required")
        @Schema(description = "Login de l'utilisateur", example = "john.doe")
        String login,

        @NotBlank(message = "Password is required")
        @Schema(description = "Mot de passe de l'utilisateur", example = "SecurePass123!")
        String password
) {}