package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for user information.
 * Used to serialize user data in API responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Représentation d'un utilisateur dans les réponses")
public record UserResponse(
        @Schema(description = "Identifiant unique de l'utilisateur", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Login de l'utilisateur", example = "jean.dupont")
        String login,

        @Schema(description = "Nom de famille de l'utilisateur", example = "Dupont")
        String lastName,

        @Schema(description = "Prénom de l'utilisateur", example = "Jean")
        String firstName,

        @Schema(description = "Date de naissance au format ISO", example = "1990-01-15")
        String birthDate,

        @Schema(description = "Âge calculé de l'utilisateur", example = "33")
        Integer age,

        @Schema(description = "Indique si l'utilisateur est majeur", example = "true")
        Boolean isAdult,

        @Schema(description = "Liste des adresses email de l'utilisateur")
        List<String> emails,

        @Schema(description = "Liste des numéros de téléphone de l'utilisateur")
        List<String> phoneNumbers
) {}
