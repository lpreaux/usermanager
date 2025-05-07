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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserRepositoryAdapter.
 * On teste la conversion entre les entités JPA et le modèle de domaine.
 */
@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UserRepositoryAdapter adapter;

    private User domainUser;
    private UserEntity entityUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        // Setup domain user
        domainUser = User.builder()
                .id(UserId.of(userId))
                .login(Login.of("john.doe"))
                .password(Password.of("SecurePass123!"))
                .lastName(Name.of("Doe"))
                .firstName(FirstName.of("John"))
                .birthDate(BirthDate.of(1990, 5, 15))
                .emails(List.of(Email.of("john.doe@example.com")))
                .phoneNumbers(List.of(PhoneNumber.of("+33612345678")))
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
    @DisplayName("Should save user and map correctly")
    void shouldSaveUserAndMapCorrectly() {
        // Given
        when(userJpaRepository.save(any(UserEntity.class))).thenReturn(entityUser);

        // When
        User result = adapter.save(domainUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId().getValue()).isEqualTo(userId);
        assertThat(result.getLogin().getValue()).isEqualTo("john.doe");
        assertThat(result.getEmails()).hasSize(1);
        assertThat(result.getPhoneNumbers()).hasSize(1);

        verify(userJpaRepository).save(argThat(entity ->
                entity.getLogin().equals("john.doe") &&
                        entity.getEmails().size() == 1 &&
                        entity.getEmails().get(0).getEmail().equals("john.doe@example.com")
        ));
    }

    @Test
    @DisplayName("Should find user by ID and map correctly")
    void shouldFindUserByIdAndMapCorrectly() {
        // Given
        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(entityUser));

        // When
        Optional<User> result = adapter.findById(UserId.of(userId));

        // Then
        assertThat(result).isPresent();
        User user = result.get();
        assertThat(user.getId().getValue()).isEqualTo(userId);
        assertThat(user.getLogin().getValue()).isEqualTo("john.doe");
        assertThat(user.getEmails()).hasSize(1);
        assertThat(user.getEmails().get(0).getValue()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should return false when login does not exist")
    void shouldReturnFalseWhenLoginDoesNotExist() {
        // Given
        when(userJpaRepository.existsByLogin("nonexistent")).thenReturn(false);

        // When
        boolean exists = adapter.existsByLogin(Login.of("nonexistent"));

        // Then
        assertThat(exists).isFalse();
        verify(userJpaRepository).existsByLogin("nonexistent");
    }

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        // Given
        when(userJpaRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When
        boolean exists = adapter.existsByEmail(Email.of("existing@example.com"));

        // Then
        assertThat(exists).isTrue();
        verify(userJpaRepository).existsByEmail("existing@example.com");
    }

    @Test
    @DisplayName("Should map multiple emails correctly")
    void shouldMapMultipleEmailsCorrectly() {
        // Given
        UserEmailEntity email1 = UserEmailEntity.builder()
                .email("john.doe@example.com")
                .user(entityUser)
                .build();
        UserEmailEntity email2 = UserEmailEntity.builder()
                .email("john.secondary@example.com")
                .user(entityUser)
                .build();
        entityUser.setEmails(List.of(email1, email2));

        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(entityUser));

        // When
        Optional<User> result = adapter.findById(UserId.of(userId));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmails()).hasSize(2);
        assertThat(result.get().getEmails().stream()
                .map(Email::getValue))
                .containsExactlyInAnyOrder("john.doe@example.com", "john.secondary@example.com");
    }
}