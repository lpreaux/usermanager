package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Requête d'enregistrement d'un nouvel utilisateur")
public record RegisterUserRequest(
        @NotBlank(message = "Login is required")
        @Size(min = 4, max = 50, message = "Login must be between 4 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Login can only contain letters, numbers, dots, hyphens, and underscores")
        @Schema(description = "Login unique de l'utilisateur", example = "jean.dupont", required = true)
        String login,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Schema(description = "Mot de passe sécurisé (min 8 caractères, majuscules, minuscules, chiffres et caractères spéciaux)",
                example = "SecurePass123!", required = true)
        String password,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        @Schema(description = "Nom de famille", example = "Dupont", required = true)
        String lastName,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        @Schema(description = "Prénom", example = "Jean", required = true)
        String firstName,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        @Schema(description = "Date de naissance", example = "1990-01-15", required = true)
        LocalDate birthDate,

        @NotEmpty(message = "At least one email is required")
        @Schema(description = "Liste des adresses email (au moins une)", example = "[\"jean.dupont@example.com\"]", required = true)
        List<@Email(message = "Invalid email format")
        @NotBlank(message = "Email cannot be empty") String> emails,

        @Schema(description = "Liste des numéros de téléphone (optionnel)", example = "[\"+33612345678\"]")
        List<@Pattern(regexp = "^\\+?[0-9\\s-]{6,20}$", message = "Invalid phone number format") String> phoneNumbers
) {}
