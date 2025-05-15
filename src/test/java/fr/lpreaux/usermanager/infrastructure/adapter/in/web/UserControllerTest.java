package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lpreaux.usermanager.application.exception.UserNotFoundException;
import fr.lpreaux.usermanager.application.port.in.*;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase.UserDetailsDTO;
import fr.lpreaux.usermanager.domain.model.valueobject.UserId;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.RegisterUserRequest;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.UpdatePersonalInfoRequest;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.mapper.UserWebMapper;
import fr.lpreaux.usermanager.infrastructure.adapter.out.analytics.AnalyticsService;
import fr.lpreaux.usermanager.infrastructure.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour UserController.
 * Dans l'architecture hexagonale, on teste les adaptateurs web avec MockMvc.
 */
@WebMvcTest(UserController.class)
@Import({UserWebMapper.class, TestSecurityConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private UserQueryUseCase userQueryUseCase;

    @MockBean
    private UpdateUserUseCase updateUserUseCase;

    @MockBean
    private DeleteUserUseCase deleteUserUseCase;

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "john.doe",
                "SecurePass123!",
                "Doe",
                "John",
                LocalDate.of(1990, 5, 15),
                List.of("john.doe@example.com"),
                List.of("+33612345678")
        );

        UUID userId = UUID.randomUUID();
        UserDetailsDTO userDetails = new UserDetailsDTO(
                userId.toString(),
                "john.doe",
                "Doe",
                "John",
                "1990-05-15",
                33,
                true,
                List.of("john.doe@example.com"),
                List.of("+33612345678")
        );

        when(registerUserUseCase.registerUser(any())).thenReturn(UserId.of(userId));
        when(userQueryUseCase.findUserById(userId.toString())).thenReturn(Optional.of(userDetails));

        // When/Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.login").value("john.doe"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.emails[0]").value("john.doe@example.com"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("Should return 400 for invalid registration request")
    void shouldReturn400ForInvalidRegistrationRequest() throws Exception {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "", // Invalid: empty login
                "weak", // Invalid: too short password
                "",  // Invalid: empty last name
                "",  // Invalid: empty first name
                LocalDate.now().plusDays(1), // Invalid: future date
                List.of(), // Invalid: empty emails
                null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.login").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists())
                .andExpect(jsonPath("$.errors.emails").exists());
    }

    @Test
    @DisplayName("Should get user by ID")
    void shouldGetUserById() throws Exception {
        // Given
        String userId = UUID.randomUUID().toString();
        UserDetailsDTO userDetails = new UserDetailsDTO(
                userId,
                "john.doe",
                "Doe",
                "John",
                "1990-05-15",
                33,
                true,
                List.of("john.doe@example.com"),
                List.of("+33612345678")
        );

        when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(userDetails));

        // When/Then
        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.login").value("john.doe"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.all-users.href").exists());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        // Given
        String userId = UUID.randomUUID().toString();
        when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() throws Exception {
        // Given
        String userId = UUID.randomUUID().toString();

        // When/Then
        mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should handle UserNotFoundException when deleting")
    void shouldHandleUserNotFoundExceptionWhenDeleting() throws Exception {
        // Given
        String userId = UUID.randomUUID().toString();
        doThrow(new UserNotFoundException("User not found with ID: " + userId))
                .when(deleteUserUseCase).deleteUser(userId);

        // When/Then
        mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"))
                .andExpect(jsonPath("$.detail").value("User not found with ID: " + userId));
    }

    @Test
    @DisplayName("Should search user by email")
    void shouldSearchUserByEmail() throws Exception {
        // Given
        String email = "john.doe@example.com";
        UserDetailsDTO userDetails = new UserDetailsDTO(
                UUID.randomUUID().toString(),
                "john.doe",
                "Doe",
                "John",
                "1990-05-15",
                33,
                true,
                List.of(email),
                List.of("+33612345678")
        );

        when(userQueryUseCase.findUserByEmail(email)).thenReturn(Optional.of(userDetails));

        // When/Then
        mockMvc.perform(get("/api/v1/users/search/by-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("john.doe"))
                .andExpect(jsonPath("$.emails[0]").value(email))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("Should update personal info successfully")
    void shouldUpdatePersonalInfoSuccessfully() throws Exception {
        // Given
        String userId = UUID.randomUUID().toString();
        UpdatePersonalInfoRequest request = new UpdatePersonalInfoRequest(
                "Smith",
                "Jane",
                LocalDate.of(1992, 8, 20)
        );

        UserDetailsDTO updatedUserDetails = new UserDetailsDTO(
                userId,
                "john.doe",
                "Smith",
                "Jane",
                "1992-08-20",
                31,
                true,
                List.of("john.doe@example.com"),
                List.of("+33612345678")
        );

        when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(updatedUserDetails));

        // When/Then
        mockMvc.perform(put("/api/v1/users/{userId}/personal-info", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.birthDate").value("1992-08-20"));
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() throws Exception {
        // Given
        List<UserDetailsDTO> users = List.of(
                new UserDetailsDTO(
                        UUID.randomUUID().toString(),
                        "john.doe",
                        "Doe",
                        "John",
                        "1990-05-15",
                        33,
                        true,
                        List.of("john.doe@example.com"),
                        List.of("+33612345678")
                ),
                new UserDetailsDTO(
                        UUID.randomUUID().toString(),
                        "jane.smith",
                        "Smith",
                        "Jane",
                        "1992-08-20",
                        31,
                        true,
                        List.of("jane.smith@example.com"),
                        List.of("+33687654321")
                )
        );

        when(userQueryUseCase.getAllUsers()).thenReturn(users);

        // When/Then
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userResponseList").isArray())
                .andExpect(jsonPath("$._embedded.userResponseList[0].login").value("john.doe"))
                .andExpect(jsonPath("$._embedded.userResponseList[1].login").value("jane.smith"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }
}