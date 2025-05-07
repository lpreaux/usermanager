package fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Response DTO for user information.
 * Used to serialize user data in API responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        String id,
        String login,
        String lastName,
        String firstName,
        String birthDate,
        Integer age,
        Boolean isAdult,
        List<String> emails,
        List<String> phoneNumbers
) {}
