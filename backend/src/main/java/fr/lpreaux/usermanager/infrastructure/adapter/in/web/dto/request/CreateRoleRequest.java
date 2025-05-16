package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateRoleRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        @Schema(description = "Nom du rôle", example = "MODERATOR")
        String name,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description cannot exceed 255 characters")
        @Schema(description = "Description du rôle", example = "Moderator with limited administrative privileges")
        String description,

        @Schema(description = "Ensemble des permissions associées au rôle", example = "[\"USER_READ\", \"USER_UPDATE\"]")
        Set<String> permissions
) {}