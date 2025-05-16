package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UpdatePersonalInfoRequest(
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate
) {}
