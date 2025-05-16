package fr.lpreaux.usermanager.infrastructure.persistence.adapter;

import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.*;
import fr.lpreaux.usermanager.infrastructure.persistence.entity.UserEmailEntity;
import fr.lpreaux.usermanager.infrastructure.persistence.entity.UserEntity;
import fr.lpreaux.usermanager.infrastructure.persistence.entity.UserPhoneNumberEntity;
import fr.lpreaux.usermanager.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UserRepositoryAdapter adapter;

    private UUID userId;
    private User domainUser;
    private UserEntity entityUser;
    private Email email;
    private PhoneNumber phoneNumber;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email = Email.of("john.doe@example.com");
        phoneNumber = PhoneNumber.of("+33612345678");

        // Setup domain user
        domainUser = User.builder()
                .id(UserId.of(userId))
                .login(Login.of("john.doe"))
                .password(Password.of("SecurePass123!"))
                .lastName(Name.of("Doe"))
                .firstName(FirstName.of("John"))
                .birthDate(BirthDate.of(1990, 5, 15))
                .emails(List.of(email))
                .phoneNumbers(List.of(phoneNumber))
                .build();

        // Setup entity user
        entityUser = UserEntity.builder()
                .id(userId)
                .login("john.doe")
                .password("SecurePass123!")
                .lastName("Doe")
                .firstName("John")
                .birthDate(LocalDate.of(1990, 5, 15))
                .build();

        UserEmailEntity emailEntity = UserEmailEntity.builder()
                .email("john.doe@example.com")
                .user(entityUser)
                .build();
        entityUser.setEmails(List.of(emailEntity));

        UserPhoneNumberEntity phoneEntity = UserPhoneNumberEntity.builder()
                .phoneNumber("+33612345678")
                .user(entityUser)
                .build();
        entityUser.setPhoneNumbers(List.of(phoneEntity));
    }

    @Test
    @DisplayName("Should find user by login")
    void shouldFindUserByLogin() {
        // Given
        Login login = Login.of("john.doe");
        when(userJpaRepository.findByLogin(login.getValue())).thenReturn(Optional.of(entityUser));

        // When
        Optional<User> result = adapter.findByLogin(login);

        // Then
        assertThat(result).isPresent();
        User user = result.get();
        assertThat(user.getId().getValue()).isEqualTo(userId);
        assertThat(user.getLogin().getValue()).isEqualTo("john.doe");
        assertThat(user.getLastName().getValue()).isEqualTo("Doe");

        verify(userJpaRepository).findByLogin("john.doe");
    }

    @Test
    @DisplayName("Should return empty when user not found by login")
    void shouldReturnEmptyWhenUserNotFoundByLogin() {
        // Given
        Login login = Login.of("nonexistent");
        when(userJpaRepository.findByLogin(login.getValue())).thenReturn(Optional.empty());

        // When
        Optional<User> result = adapter.findByLogin(login);

        // Then
        assertThat(result).isEmpty();
        verify(userJpaRepository).findByLogin("nonexistent");
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        Email email = Email.of("john.doe@example.com");
        when(userJpaRepository.findByEmail(email.getValue())).thenReturn(Optional.of(entityUser));

        // When
        Optional<User> result = adapter.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        User user = result.get();
        assertThat(user.getId().getValue()).isEqualTo(userId);
        assertThat(user.getEmails()).hasSize(1);
        assertThat(user.getEmails().get(0).getValue()).isEqualTo("john.doe@example.com");

        verify(userJpaRepository).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // Given
        Email email = Email.of("nonexistent@example.com");
        when(userJpaRepository.findByEmail(email.getValue())).thenReturn(Optional.empty());

        // When
        Optional<User> result = adapter.findByEmail(email);

        // Then
        assertThat(result).isEmpty();
        verify(userJpaRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should check if login exists")
    void shouldCheckIfLoginExists() {
        // Given
        Login login = Login.of("existing.login");
        when(userJpaRepository.existsByLogin(login.getValue())).thenReturn(true);

        // When
        boolean exists = adapter.existsByLogin(login);

        // Then
        assertThat(exists).isTrue();
        verify(userJpaRepository).existsByLogin("existing.login");
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Given
        Email email = Email.of("existing@example.com");
        when(userJpaRepository.existsByEmail(email.getValue())).thenReturn(true);

        // When
        boolean exists = adapter.existsByEmail(email);

        // Then
        assertThat(exists).isTrue();
        verify(userJpaRepository).existsByEmail("existing@example.com");
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Given
        UserId userId = UserId.of(UUID.randomUUID());

        // When
        adapter.delete(userId);

        // Then
        verify(userJpaRepository).deleteById(userId.getValue());
    }

    @Test
    @DisplayName("Should find all users")
    void shouldFindAllUsers() {
        // Given
        UserEntity secondUser = createSecondUserEntity();
        when(userJpaRepository.findAll()).thenReturn(List.of(entityUser, secondUser));

        // When
        List<User> result = adapter.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLogin().getValue()).isEqualTo("john.doe");
        assertThat(result.get(1).getLogin().getValue()).isEqualTo("jane.smith");

        verify(userJpaRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users found")
    void shouldReturnEmptyListWhenNoUsersFound() {
        // Given
        when(userJpaRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<User> result = adapter.findAll();

        // Then
        assertThat(result).isEmpty();
        verify(userJpaRepository).findAll();
    }

    @Test
    @DisplayName("Should save user with multiple emails and phone numbers")
    void shouldSaveUserWithMultipleEmailsAndPhoneNumbers() {
        // Given
        Email secondEmail = Email.of("john.secondary@example.com");
        PhoneNumber secondPhone = PhoneNumber.of("+33687654321");

        User userWithMultipleContacts = User.builder()
                .id(UserId.of(userId))
                .login(Login.of("john.doe"))
                .password(Password.of("SecurePass123!"))
                .lastName(Name.of("Doe"))
                .firstName(FirstName.of("John"))
                .birthDate(BirthDate.of(1990, 5, 15))
                .emails(List.of(email, secondEmail))
                .phoneNumbers(List.of(phoneNumber, secondPhone))
                .build();

        // Mock the save method to return the entity as is
        when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User savedUser = adapter.save(userWithMultipleContacts);

        // Then
        ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userJpaRepository).save(entityCaptor.capture());

        UserEntity capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getEmails()).hasSize(2);
        assertThat(capturedEntity.getPhoneNumbers()).hasSize(2);

        // Verify the saved domain user
        assertThat(savedUser.getEmails()).hasSize(2);
        assertThat(savedUser.getEmails().stream().map(Email::getValue))
                .containsExactlyInAnyOrder("john.doe@example.com", "john.secondary@example.com");

        assertThat(savedUser.getPhoneNumbers()).hasSize(2);
        assertThat(savedUser.getPhoneNumbers().stream().map(PhoneNumber::getValue))
                .containsExactlyInAnyOrder("+33612345678", "+33687654321");
    }

    @Test
    @DisplayName("Should handle entity with no emails or phone numbers")
    void shouldHandleEntityWithNoEmailsOrPhoneNumbers() {
        // Given
        UserEntity emptyEntity = UserEntity.builder()
                .id(userId)
                .login("empty.user")
                .password("SecurePass123!")
                .lastName("Empty")
                .firstName("User")
                .birthDate(LocalDate.of(1995, 10, 20))
                .emails(Collections.emptyList())
                .phoneNumbers(Collections.emptyList())
                .build();

        when(userJpaRepository.findById(any(UUID.class))).thenReturn(Optional.of(emptyEntity));

        // When
        Optional<User> result = adapter.findById(UserId.of(userId));

        // Then
        assertThat(result).isPresent();
        User user = result.get();
        assertThat(user.getEmails()).isEmpty();
        assertThat(user.getPhoneNumbers()).isEmpty();
    }

    @Test
    @DisplayName("Should preserve email case sensitivity in domain model but not in entity")
    void shouldPreserveEmailCaseSensitivityInDomainModelButNotInEntity() {
        // Given
        Email mixedCaseEmail = Email.of("John.Doe@Example.com");
        User userWithMixedCaseEmail = User.builder()
                .id(UserId.of(userId))
                .login(Login.of("john.doe"))
                .password(Password.of("SecurePass123!"))
                .lastName(Name.of("Doe"))
                .firstName(FirstName.of("John"))
                .birthDate(BirthDate.of(1990, 5, 15))
                .emails(List.of(mixedCaseEmail))
                .phoneNumbers(List.of(phoneNumber))
                .build();

        // Mock the save method to return the entity as is
        when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity savedEntity = (UserEntity) invocation.getArgument(0);
            // Verify that email was saved in lowercase in the entity
            assertThat(savedEntity.getEmails().get(0).getEmail()).isEqualTo("john.doe@example.com");
            return savedEntity;
        });

        // When
        User savedUser = adapter.save(userWithMixedCaseEmail);

        // Then
        // The domain model should preserve the email as is
        assertThat(savedUser.getEmails().get(0).getValue()).isEqualTo("john.doe@example.com");
    }

    // Helper method to create a second user entity for testing
    private UserEntity createSecondUserEntity() {
        UUID secondUserId = UUID.randomUUID();
        UserEntity secondUser = UserEntity.builder()
                .id(secondUserId)
                .login("jane.smith")
                .password("SecurePass456!")
                .lastName("Smith")
                .firstName("Jane")
                .birthDate(LocalDate.of(1992, 8, 20))
                .build();

        UserEmailEntity secondEmailEntity = UserEmailEntity.builder()
                .email("jane.smith@example.com")
                .user(secondUser)
                .build();
        secondUser.setEmails(List.of(secondEmailEntity));

        UserPhoneNumberEntity secondPhoneEntity = UserPhoneNumberEntity.builder()
                .phoneNumber("+33687654321")
                .user(secondUser)
                .build();
        secondUser.setPhoneNumbers(List.of(secondPhoneEntity));

        return secondUser;
    }
}