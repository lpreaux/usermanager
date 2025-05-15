package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lpreaux.usermanager.application.exception.EmailAlreadyExistsException;
import fr.lpreaux.usermanager.application.exception.InvalidPasswordException;
import fr.lpreaux.usermanager.application.exception.LastEmailException;
import fr.lpreaux.usermanager.application.exception.UserNotFoundException;
import fr.lpreaux.usermanager.application.port.in.*;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase.UserDetailsDTO;
import fr.lpreaux.usermanager.domain.model.valueobject.UserId;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.*;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.mapper.UserWebMapper;
import fr.lpreaux.usermanager.infrastructure.adapter.out.analytics.AnalyticsService;
import fr.lpreaux.usermanager.infrastructure.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users")
        void shouldReturnAllUsers() throws Exception {
            // Given
            List<UserDetailsDTO> users = List.of(
                    createUserDetailsDTO(UUID.randomUUID().toString(), "user1"),
                    createUserDetailsDTO(UUID.randomUUID().toString(), "user2")
            );

            when(userQueryUseCase.getAllUsers()).thenReturn(users);

            // When/Then
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaTypes.HAL_JSON))
                    .andExpect(jsonPath("$._embedded.userResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.userResponseList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.userResponseList[0].login").value("user1"))
                    .andExpect(jsonPath("$._embedded.userResponseList[1].login").value("user2"))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(userQueryUseCase).getAllUsers();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsersExist() throws Exception {
            // Given
            when(userQueryUseCase.getAllUsers()).thenReturn(Collections.emptyList());

            // When/Then
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaTypes.HAL_JSON))
                    .andExpect(jsonPath("$._embedded").doesNotExist())
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(userQueryUseCase).getAllUsers();
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "CurrentPass123!",
                    "NewSecurePass456!"
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/password/change", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(updateUserUseCase).changePassword(any(UpdateUserUseCase.ChangePasswordCommand.class));
        }

        @Test
        @DisplayName("Should return 400 when current password is incorrect")
        void shouldReturn400WhenCurrentPasswordIsIncorrect() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "WrongPassword123!",
                    "NewSecurePass456!"
            );

            doThrow(new InvalidPasswordException("Current password is incorrect"))
                    .when(updateUserUseCase).changePassword(any(UpdateUserUseCase.ChangePasswordCommand.class));

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/password/change", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Password"))
                    .andExpect(jsonPath("$.detail").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "CurrentPass123!",
                    "NewSecurePass456!"
            );

            doThrow(new UserNotFoundException("User not found with ID: " + userId))
                    .when(updateUserUseCase).changePassword(any(UpdateUserUseCase.ChangePasswordCommand.class));

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/password/change", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("User Not Found"))
                    .andExpect(jsonPath("$.detail").value("User not found with ID: " + userId));
        }

        @Test
        @DisplayName("Should return 400 when password format is invalid")
        void shouldReturn400WhenPasswordFormatIsInvalid() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "CurrentPass123!",
                    "weak" // Too short password
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/password/change", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.newPassword").exists());
        }
    }

    @Nested
    @DisplayName("Email Management Tests")
    class EmailManagementTests {

        @Test
        @DisplayName("Should add email successfully")
        void shouldAddEmailSuccessfully() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            AddEmailRequest request = new AddEmailRequest("new.email@example.com");

            UserDetailsDTO userDetails = createUserDetailsDTO(userId, "user1");
            when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(userDetails));

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/emails", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(updateUserUseCase).addEmail(any(UpdateUserUseCase.AddEmailCommand.class));
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            AddEmailRequest request = new AddEmailRequest("existing@example.com");

            doThrow(new EmailAlreadyExistsException("Email already exists: existing@example.com"))
                    .when(updateUserUseCase).addEmail(any(UpdateUserUseCase.AddEmailCommand.class));

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/emails", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Email Already Exists"))
                    .andExpect(jsonPath("$.detail").value("Email already exists: existing@example.com"));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailFormatIsInvalid() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            AddEmailRequest request = new AddEmailRequest("invalid-email");

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/emails", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @DisplayName("Should remove email successfully")
        void shouldRemoveEmailSuccessfully() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            String email = "remove.me@example.com";

            UserDetailsDTO userDetails = createUserDetailsDTO(userId, "user1");
            when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(userDetails));

            // When/Then
            mockMvc.perform(delete("/api/v1/users/{userId}/emails/{email}", userId, email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(updateUserUseCase).removeEmail(any(UpdateUserUseCase.RemoveEmailCommand.class));
        }

        @Test
        @DisplayName("Should return 400 when trying to remove last email")
        void shouldReturn400WhenTryingToRemoveLastEmail() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            String email = "last.email@example.com";

            doThrow(new LastEmailException("Cannot remove the last email address"))
                    .when(updateUserUseCase).removeEmail(any(UpdateUserUseCase.RemoveEmailCommand.class));

            // When/Then
            mockMvc.perform(delete("/api/v1/users/{userId}/emails/{email}", userId, email))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Cannot Remove Last Email"))
                    .andExpect(jsonPath("$.detail").value("Cannot remove the last email address"));
        }
    }

    @Nested
    @DisplayName("Phone Number Management Tests")
    class PhoneNumberManagementTests {

        @Test
        @DisplayName("Should add phone number successfully")
        void shouldAddPhoneNumberSuccessfully() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            AddPhoneNumberRequest request = new AddPhoneNumberRequest("+33612345678");

            UserDetailsDTO userDetails = createUserDetailsDTO(userId, "user1");
            when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(userDetails));

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/phone-numbers", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(updateUserUseCase).addPhoneNumber(any(UpdateUserUseCase.AddPhoneNumberCommand.class));
        }

        @Test
        @DisplayName("Should return 400 when phone number format is invalid")
        void shouldReturn400WhenPhoneNumberFormatIsInvalid() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            AddPhoneNumberRequest request = new AddPhoneNumberRequest("invalid");

            // When/Then
            mockMvc.perform(post("/api/v1/users/{userId}/phone-numbers", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.phoneNumber").exists());
        }

        @Test
        @DisplayName("Should remove phone number successfully")
        void shouldRemovePhoneNumberSuccessfully() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            String phoneNumber = "+33612345678";

            UserDetailsDTO userDetails = createUserDetailsDTO(userId, "user1");
            when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(userDetails));

            // When/Then
            mockMvc.perform(delete("/api/v1/users/{userId}/phone-numbers/{phoneNumber}", userId, phoneNumber))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$._links.self.href").exists());

            verify(updateUserUseCase).removePhoneNumber(any(UpdateUserUseCase.RemovePhoneNumberCommand.class));
        }
    }

    @Nested
    @DisplayName("User Search Tests")
    class UserSearchTests {

        @Test
        @DisplayName("Should find user by login")
        void shouldFindUserByLogin() throws Exception {
            // Given
            String login = "john.doe";
            UserDetailsDTO userDetails = createUserDetailsDTO(UUID.randomUUID().toString(), login);

            when(userQueryUseCase.findUserByLogin(login)).thenReturn(Optional.of(userDetails));

            // When/Then
            mockMvc.perform(get("/api/v1/users/search/by-login")
                            .param("login", login))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(login))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.search-by-login.href").exists());

            verify(userQueryUseCase).findUserByLogin(login);
        }

        @Test
        @DisplayName("Should return 404 when user not found by login")
        void shouldReturn404WhenUserNotFoundByLogin() throws Exception {
            // Given
            String login = "nonexistent";

            when(userQueryUseCase.findUserByLogin(login)).thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/v1/users/search/by-login")
                            .param("login", login))
                    .andExpect(status().isNotFound());

            verify(userQueryUseCase).findUserByLogin(login);
        }
    }

    @Nested
    @DisplayName("Registration Validation Tests")
    class RegistrationValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"a", "ab", "abc"})
        @DisplayName("Should reject registration with too short login")
        void shouldRejectRegistrationWithTooShortLogin(String shortLogin) throws Exception {
            // Given
            RegisterUserRequest request = createValidRegisterRequest();
            request = new RegisterUserRequest(
                    shortLogin,
                    request.password(),
                    request.lastName(),
                    request.firstName(),
                    request.birthDate(),
                    request.emails(),
                    request.phoneNumbers()
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.login").exists());
        }

        @Test
        @DisplayName("Should reject registration with invalid email format")
        void shouldRejectRegistrationWithInvalidEmailFormat() throws Exception {
            // Given
            RegisterUserRequest request = createValidRegisterRequest();
            request = new RegisterUserRequest(
                    request.login(),
                    request.password(),
                    request.lastName(),
                    request.firstName(),
                    request.birthDate(),
                    List.of("invalid-email"),
                    request.phoneNumbers()
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    // Corriger le chemin JsonPath pour correspondre au format réel
                    .andExpect(jsonPath("$.errors['emails[0]']").exists());
        }

        @Test
        @DisplayName("Should reject registration with future birth date")
        void shouldRejectRegistrationWithFutureBirthDate() throws Exception {
            // Given
            RegisterUserRequest request = createValidRegisterRequest();
            request = new RegisterUserRequest(
                    request.login(),
                    request.password(),
                    request.lastName(),
                    request.firstName(),
                    LocalDate.now().plusYears(1),
                    request.emails(),
                    request.phoneNumbers()
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors.birthDate").exists());
        }

        @Test
        @DisplayName("Should reject registration with invalid phone number")
        void shouldRejectRegistrationWithInvalidPhoneNumber() throws Exception {
            // Given
            RegisterUserRequest request = createValidRegisterRequest();
            request = new RegisterUserRequest(
                    request.login(),
                    request.password(),
                    request.lastName(),
                    request.firstName(),
                    request.birthDate(),
                    request.emails(),
                    List.of("invalid")
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    // Corriger le chemin JsonPath pour correspondre au format réel
                    .andExpect(jsonPath("$.errors['phoneNumbers[0]']").exists());
        }
    }

    @Nested
    @DisplayName("Analytics Integration Tests")
    class AnalyticsIntegrationTests {

        @Test
        @DisplayName("Should track event when registering user")
        void shouldTrackEventWhenRegisteringUser() throws Exception {
            // Given
            RegisterUserRequest request = createValidRegisterRequest();
            String userId = UUID.randomUUID().toString();

            when(registerUserUseCase.registerUser(any())).thenReturn(UserId.of(UUID.fromString(userId)));
            when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(createUserDetailsDTO(userId, request.login())));

            // When/Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(analyticsService).trackEvent(eq(userId), eq("user_registered"), any());
            verify(analyticsService).identifyUser(eq(userId), any());
        }

        @Test
        @DisplayName("Should track event when deleting user")
        void shouldTrackEventWhenDeletingUser() throws Exception {
            // Given
            String userId = UUID.randomUUID().toString();
            UserDetailsDTO userDetails = createUserDetailsDTO(userId, "user.to.delete");

            when(userQueryUseCase.findUserById(userId)).thenReturn(Optional.of(userDetails));

            // When/Then
            mockMvc.perform(delete("/api/v1/users/{userId}", userId))
                    .andExpect(status().isNoContent());

            verify(analyticsService).trackEvent(eq(userId), eq("user_deleted"), any());
        }
    }

    // Helper methods

    private UserDetailsDTO createUserDetailsDTO(String userId, String login) {
        return new UserDetailsDTO(
                userId,
                login,
                "Doe",
                "John",
                "1990-05-15",
                33,
                true,
                List.of(login + "@example.com"),
                List.of("+33612345678")
        );
    }

    private RegisterUserRequest createValidRegisterRequest() {
        return new RegisterUserRequest(
                "john.doe",
                "SecurePass123!",
                "Doe",
                "John",
                LocalDate.of(1990, 5, 15),
                List.of("john.doe@example.com"),
                List.of("+33612345678")
        );
    }
}