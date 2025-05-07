package fr.lpreaux.usermanager.domain.model;

import fr.lpreaux.usermanager.domain.model.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour l'entité User.
 * Dans l'architecture hexagonale, on teste le domaine sans dépendances externes.
 */
class UserTest {

    private UserId userId;
    private Login login;
    private Password password;
    private Name lastName;
    private FirstName firstName;
    private BirthDate birthDate;
    private List<Email> emails;
    private List<PhoneNumber> phoneNumbers;

    @BeforeEach
    void setUp() {
        userId = UserId.generate();
        login = Login.of("john.doe");
        password = Password.of("SecurePass123!");
        lastName = Name.of("Doe");
        firstName = FirstName.of("John");
        birthDate = BirthDate.of(1990, 5, 15);
        emails = List.of(Email.of("john.doe@example.com"));
        phoneNumbers = List.of(PhoneNumber.of("+33612345678"));
    }

    @Test
    @DisplayName("Should create a user with valid data")
    void shouldCreateUserWithValidData() {
        // When
        User user = User.create(login, password, lastName, firstName,
                birthDate, emails, phoneNumbers);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getLogin()).isEqualTo(login);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getLastName()).isEqualTo(lastName);
        assertThat(user.getFirstName()).isEqualTo(firstName);
        assertThat(user.getBirthDate()).isEqualTo(birthDate);
        assertThat(user.getEmails()).containsExactlyElementsOf(emails);
        assertThat(user.getPhoneNumbers()).containsExactlyElementsOf(phoneNumbers);
    }

    @Test
    @DisplayName("Should update personal information")
    void shouldUpdatePersonalInformation() {
        // Given
        User user = User.create(login, password, lastName, firstName,
                birthDate, emails, phoneNumbers);
        Name newLastName = Name.of("Smith");
        FirstName newFirstName = FirstName.of("Jane");
        BirthDate newBirthDate = BirthDate.of(1992, 8, 20);

        // When
        User updatedUser = user.updatePersonalInformation(newLastName,
                newFirstName, newBirthDate);

        // Then
        assertThat(updatedUser.getId()).isEqualTo(user.getId());
        assertThat(updatedUser.getLastName()).isEqualTo(newLastName);
        assertThat(updatedUser.getFirstName()).isEqualTo(newFirstName);
        assertThat(updatedUser.getBirthDate()).isEqualTo(newBirthDate);
        // Login et mot de passe ne changent pas
        assertThat(updatedUser.getLogin()).isEqualTo(login);
        assertThat(updatedUser.getPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("Should add email to user")
    void shouldAddEmailToUser() {
        // Given
        User user = User.create(login, password, lastName, firstName,
                birthDate, new ArrayList<>(emails), phoneNumbers);
        Email newEmail = Email.of("john.smith@example.com");

        // When
        User updatedUser = user.addEmail(newEmail);

        // Then
        assertThat(updatedUser.getEmails()).hasSize(2);
        assertThat(updatedUser.getEmails()).contains(newEmail);
    }

    @Test
    @DisplayName("Should remove email from user")
    void shouldRemoveEmailFromUser() {
        // Given
        List<Email> multipleEmails = new ArrayList<>();
        multipleEmails.add(Email.of("john.doe@example.com"));
        multipleEmails.add(Email.of("john.secondary@example.com"));

        User user = User.create(login, password, lastName, firstName,
                birthDate, multipleEmails, phoneNumbers);
        Email emailToRemove = Email.of("john.secondary@example.com");

        // When
        User updatedUser = user.removeEmail(emailToRemove);

        // Then
        assertThat(updatedUser.getEmails()).hasSize(1);
        assertThat(updatedUser.getEmails()).doesNotContain(emailToRemove);
    }

    @Test
    @DisplayName("Should update password")
    void shouldUpdatePassword() {
        // Given
        User user = User.create(login, password, lastName, firstName,
                birthDate, emails, phoneNumbers);
        Password newPassword = Password.of("NewSecurePass456!");

        // When
        User updatedUser = user.updatePassword(newPassword);

        // Then
        assertThat(updatedUser.getPassword()).isEqualTo(newPassword);
        assertThat(updatedUser.getPassword()).isNotEqualTo(password);
    }

    @Test
    @DisplayName("Should return unmodifiable email list")
    void shouldReturnUnmodifiableEmailList() {
        // Given
        User user = User.create(login, password, lastName, firstName,
                birthDate, emails, phoneNumbers);

        // When / Then
        assertThat(user.getEmails())
                .isUnmodifiable()
                .containsExactlyElementsOf(emails);
    }
}