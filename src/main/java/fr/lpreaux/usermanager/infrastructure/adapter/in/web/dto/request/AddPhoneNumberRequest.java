package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddPhoneNumberRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9\\s-]{6,20}$", message = "Invalid phone number format")
        String phoneNumber
) {}
