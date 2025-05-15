package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.*;
import fr.lpreaux.usermanager.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import fr.lpreaux.usermanager.application.port.in.UpdateUserUseCase.*;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase.UserDetailsDTO;
import fr.lpreaux.usermanager.application.port.out.UserRepository;
import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserService.
 * Dans l'architecture hexagonale, on teste la couche application en mockant les ports de sortie.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private RegisterUserCommand validCommand;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        validCommand = new RegisterUserCommand(
                "john.doe",
                "SecurePass123!",
                "Doe",
                "John",
                LocalDate.of(1990, 5, 15),
                List.of("john.doe@example.com"),
                List.of("+33612345678")
        );

        testUser = User.builder()
                .id(UserId.of(userId))
                .login(Login.of("john.doe"))
                .password(Password.hash("SecurePass123!"))
                .lastName(Name.of("Doe"))
                .firstName(FirstName.of("John"))
                .birthDate(BirthDate.of(1990, 5, 15))
                .emails(List.of(Email.of("john.doe@example.com")))
                .phoneNumbers(List.of(PhoneNumber.of("+33612345678")))
                .build();
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
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByLogin(any(Login.class))).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.registerUser(validCommand))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already exists: john.doe@example.com");

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
    @DisplayName("Should find user by login")
    void shouldFindUserByLogin() {
        // Given
        String login = "john.doe";
        when(userRepository.findByLogin(any(Login.class))).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDetailsDTO> result = userService.findUserByLogin(login);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().login()).isEqualTo("john.doe");
        verify(userRepository).findByLogin(Login.of(login));
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDetailsDTO> result = userService.findUserByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().login()).isEqualTo("john.doe");
        verify(userRepository).findByEmail(Email.of(email));
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() {
        // Given
        User secondUser = User.builder()
                .id(UserId.generate())
                .login(Login.of("jane.smith"))
                .password(Password.hash("AnotherPassword123!"))
                .lastName(Name.of("Smith"))
                .firstName(FirstName.of("Jane"))
                .birthDate(BirthDate.of(1992, 8, 20))
                .emails(List.of(Email.of("jane.smith@example.com")))
                .phoneNumbers(List.of(PhoneNumber.of("+33687654321")))
                .build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, secondUser));

        // When
        List<UserDetailsDTO> results = userService.getAllUsers();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).login()).isEqualTo("john.doe");
        assertThat(results.get(1).login()).isEqualTo("jane.smith");
        verify(userRepository).findAll();
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
        String userId = this.userId.toString();
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
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() {
        // Given
        String userId = this.userId.toString();
        ChangePasswordCommand command = new ChangePasswordCommand(
                userId, "SecurePass123!", "NewSecurePass456!"
        );

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.changePassword(command);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when changing password with incorrect current password")
    void shouldThrowExceptionWhenChangingPasswordWithIncorrectCurrentPassword() {
        // Given
        String userId = this.userId.toString();
        ChangePasswordCommand command = new ChangePasswordCommand(
                userId, "WrongPassword123!", "NewSecurePass456!"
        );

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> userService.changePassword(command))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Current password is incorrect");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should add email successfully")
    void shouldAddEmailSuccessfully() {
        // Given
        String userId = this.userId.toString();
        String newEmail = "john.secondary@example.com";
        AddEmailCommand command = new AddEmailCommand(userId, newEmail);

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.addEmail(command);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).existsByEmail(Email.of(newEmail));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when adding email that already exists")
    void shouldThrowExceptionWhenAddingEmailThatAlreadyExists() {
        // Given
        String userId = this.userId.toString();
        String existingEmail = "existing@example.com";
        AddEmailCommand command = new AddEmailCommand(userId, existingEmail);

        when(userRepository.existsByEmail(Email.of(existingEmail))).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.addEmail(command))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already exists: " + existingEmail);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should remove email successfully")
    void shouldRemoveEmailSuccessfully() {
        // Given
        String userId = this.userId.toString();
        String emailToRemove = "john.doe@example.com";

        // Create a user with 2 emails
        User userWithMultipleEmails = User.builder()
                .id(UserId.of(this.userId))
                .login(Login.of("john.doe"))
                .password(Password.hash("SecurePass123!"))
                .lastName(Name.of("Doe"))
                .firstName(FirstName.of("John"))
                .birthDate(BirthDate.of(1990, 5, 15))
                .emails(Arrays.asList(
                        Email.of("john.doe@example.com"),
                        Email.of("john.secondary@example.com")
                ))
                .phoneNumbers(List.of(PhoneNumber.of("+33612345678")))
                .build();

        RemoveEmailCommand command = new RemoveEmailCommand(userId, emailToRemove);

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(userWithMultipleEmails));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.removeEmail(command);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).save(argThat(user ->
                user.getEmails().size() == 1 &&
                        user.getEmails().getFirst().getValue().equals("john.secondary@example.com")
        ));
    }

    @Test
    @DisplayName("Should throw exception when removing last email")
    void shouldThrowExceptionWhenRemovingLastEmail() {
        // Given
        String userId = this.userId.toString();
        String lastEmail = "john.doe@example.com";
        RemoveEmailCommand command = new RemoveEmailCommand(userId, lastEmail);

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> userService.removeEmail(command))
                .isInstanceOf(LastEmailException.class)
                .hasMessage("Cannot remove the last email address");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should add phone number successfully")
    void shouldAddPhoneNumberSuccessfully() {
        // Given
        String userId = this.userId.toString();
        String newPhone = "+33687654321";
        AddPhoneNumberCommand command = new AddPhoneNumberCommand(userId, newPhone);

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.addPhoneNumber(command);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).save(argThat(user ->
                user.getPhoneNumbers().size() == 2 &&
                        user.getPhoneNumbers().stream().anyMatch(p -> p.getValue().equals("+33687654321"))
        ));
    }

    @Test
    @DisplayName("Should remove phone number successfully")
    void shouldRemovePhoneNumberSuccessfully() {
        // Given
        String userId = this.userId.toString();
        String phoneToRemove = "+33612345678";
        RemovePhoneNumberCommand command = new RemovePhoneNumberCommand(userId, phoneToRemove);

        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.removePhoneNumber(command);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).save(argThat(user ->
                user.getPhoneNumbers().isEmpty()
        ));
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Given
        String userId = this.userId.toString();
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).findById(UserId.of(userId));
        verify(userRepository).delete(testUser.getId());
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with ID: " + userId);

        verify(userRepository, never()).delete(any(UserId.class));
    }

    @Test
    @DisplayName("Should convert emails correctly")
    void shouldConvertEmailsCorrectly() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
                "jane.smith",
                "SecurePass123!",
                "Smith",
                "Jane",
                LocalDate.of(1992, 8, 20),
                Arrays.asList("jane.smith@example.com", "jane.work@example.com"),
                List.of("+33687654321")
        );

        when(userRepository.existsByLogin(any(Login.class))).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            // Verify that the user has both emails
            assertThat(savedUser.getEmails()).hasSize(2);
            assertThat(savedUser.getEmails().stream().map(Email::getValue))
                    .containsExactlyInAnyOrder("jane.smith@example.com", "jane.work@example.com");
            return savedUser;
        });

        // When
        userService.registerUser(command);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should convert phone numbers correctly")
    void shouldConvertPhoneNumbersCorrectly() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
                "jane.smith",
                "SecurePass123!",
                "Smith",
                "Jane",
                LocalDate.of(1992, 8, 20),
                List.of("jane.smith@example.com"),
                Arrays.asList("+33687654321", "+33612345678")
        );

        when(userRepository.existsByLogin(any(Login.class))).thenReturn(false);
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            // Verify that the user has both phone numbers
            assertThat(savedUser.getPhoneNumbers()).hasSize(2);
            assertThat(savedUser.getPhoneNumbers().stream().map(PhoneNumber::getValue))
                    .containsExactlyInAnyOrder("+33687654321", "+33612345678");
            return savedUser;
        });

        // When
        userService.registerUser(command);

        // Then
        verify(userRepository).save(any(User.class));
    }
}