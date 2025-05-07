package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record RegisterUserRequest(
        @NotBlank(message = "Login is required")
        @Size(min = 4, max = 50, message = "Login must be between 4 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Login can only contain letters, numbers, dots, hyphens, and underscores")
        String login,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotEmpty(message = "At least one email is required")
        List<@Email(message = "Invalid email format")
        @NotBlank(message = "Email cannot be empty") String> emails,

        List<@Pattern(regexp = "^\\+?[0-9\\s-]{6,20}$", message = "Invalid phone number format") String> phoneNumbers
) {}
