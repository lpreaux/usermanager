package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires pour le Value Object Email.
 * Démontre les tests paramétrés et la validation des formats.
 */
class EmailTest {

    @Test
    @DisplayName("Should create valid email")
    void shouldCreateValidEmail() {
        // When
        Email email = Email.of("john.doe@example.com");

        // Then
        assertThat(email).isNotNull();
        assertThat(email.getValue()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowercase() {
        // When
        Email email = Email.of("John.Doe@Example.COM");

        // Then
        assertThat(email.getValue()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimWhitespaceFromEmail() {
        // When
        Email email = Email.of("  john.doe@example.com  ");

        // Then
        assertThat(email.getValue()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should throw exception for null email")
    void shouldThrowExceptionForNullEmail() {
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Email cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for empty email")
    void shouldThrowExceptionForEmptyEmail() {
        assertThatThrownBy(() -> Email.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception for email exceeding max length")
    void shouldThrowExceptionForEmailExceedingMaxLength() {
        // Given
        String longEmail = "a".repeat(250) + "@example.com";

        // When/Then
        assertThatThrownBy(() -> Email.of(longEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot exceed 255 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "valid.email@example.com",
            "user+tag@domain.co.uk",
            "user_name@sub.domain.com",
            "user-name@domain.org",
            "123user@domain.net"
    })
    @DisplayName("Should accept valid email formats")
    void shouldAcceptValidEmailFormats(String validEmail) {
        // When
        Email email = Email.of(validEmail);

        // Then
        assertThat(email.getValue()).isEqualTo(validEmail.toLowerCase());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid.email",
            "@domain.com",
            "user@",
            "user..name@domain.com",
            "user@domain",
            "user name@domain.com",
            "user@.com",
            ".user@domain.com",
            "user.@domain.com"
    })
    @DisplayName("Should reject invalid email formats")
    void shouldRejectInvalidEmailFormats(String invalidEmail) {
        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email format");
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        Email email1 = Email.of("john.doe@example.com");
        Email email2 = Email.of("john.doe@example.com");
        Email email3 = Email.of("jane.doe@example.com");

        // Then
        assertThat(email1).isEqualTo(email2);
        assertThat(email1).isNotEqualTo(email3);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        assertThat(email1.hashCode()).isNotEqualTo(email3.hashCode());
    }

    @Test
    @DisplayName("Should be case insensitive for equality")
    void shouldBeCaseInsensitiveForEquality() {
        // Given
        Email email1 = Email.of("john.doe@example.com");
        Email email2 = Email.of("John.Doe@Example.COM");

        // Then
        assertThat(email1).isEqualTo(email2);
    }
}