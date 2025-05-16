package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Token is required")
        @Schema(description = "Token JWT à rafraîchir")
        String token
) {}