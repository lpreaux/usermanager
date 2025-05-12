package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.UserAlreadyExistsException;
import fr.lpreaux.usermanager.application.exception.UserNotFoundException;
import fr.lpreaux.usermanager.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import fr.lpreaux.usermanager.application.port.in.UpdateUserUseCase.UpdatePersonalInfoCommand;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase.UserDetailsDTO;
import fr.lpreaux.usermanager.application.port.out.UserRepository;
import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.*;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserService.
 * Dans l'architecture hexagonale, on teste la couche application en mockant les ports de sortie.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Counter userRegistrationCounter;

    @InjectMocks
    private UserService userService;

    private RegisterUserCommand validCommand;
    private User testUser;

    @BeforeEach
    void setUp() {
        validCommand = new RegisterUserCommand(
                "john.doe",
                "SecurePass123!",
                "Doe",
                "John",
                LocalDate.of(1990, 5, 15),
                List.of("john.doe@example.com"),
                List.of("+33612345678")
        );

        testUser = User.create(
                Login.of("john.doe"),
                Password.hash("SecurePass123!"),
                Name.of("Doe"),
                FirstName.of("John"),
                BirthDate.of(1990, 5, 15),
                List.of(Email.of("john.doe@example.com")),
                List.of(PhoneNumber.of("+33612345678"))
        );
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given
        when(userRepository.existsByLogin(any(Login.class))).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserId result = userService.registerUser(validCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testUser.getId());

        verify(userRepository).existsByLogin(Login.of("john.doe"));
        verify(userRepository).existsByEmail(Email.of("john.doe@example.com"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when login already exists")
    void shouldThrowExceptionWhenLoginAlreadyExists() {
        // Given
        when(userRepository.existsByLogin(any(Login.class))).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.registerUser(validCommand))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Login already exists: john.doe");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        // Given
        String userId = UUID.randomUUID().toString();
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDetailsDTO> result = userService.findUserById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().login()).isEqualTo("john.doe");
        assertThat(result.get().lastName()).isEqualTo("Doe");
        assertThat(result.get().emails()).containsExactly("john.doe@example.com");
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void shouldReturnEmptyWhenUserNotFound() {
        // Given
        String userId = UUID.randomUUID().toString();
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        // When
        Optional<UserDetailsDTO> result = userService.findUserById(userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should update personal information")
    void shouldUpdatePersonalInformation() {
        // Given
        String userId = UUID.randomUUID().toString();
        UpdatePersonalInfoCommand command = new UpdatePersonalInfoCommand(
                userId, "Smith", "Jane", LocalDate.of(1992, 8, 20)
        );

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.updatePersonalInfo(command);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).save(argThat(user ->
                user.getLastName().getValue().equals("Smith") &&
                        user.getFirstName().getValue().equals("Jane")
        ));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        UpdatePersonalInfoCommand command = new UpdatePersonalInfoCommand(
                userId, "Smith", "Jane", LocalDate.of(1992, 8, 20)
        );

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.updatePersonalInfo(command))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with ID: " + userId);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).delete(testUser.getId());
    }
}