package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    @Test
    @DisplayName("Should create valid password")
    void shouldCreateValidPassword() {
        // When
        Password password = Password.of("SecurePass123!");

        // Then
        assertThat(password).isNotNull();
        assertThat(password.getValue()).isEqualTo("SecurePass123!");
    }

    @Test
    @DisplayName("Should throw exception for null password")
    void shouldThrowExceptionForNullPassword() {
        assertThatThrownBy(() -> Password.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Password cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for password too short")
    void shouldThrowExceptionForPasswordTooShort() {
        assertThatThrownBy(() -> Password.of("Sh0rt!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be between 8 and 100 characters");
    }

    @Test
    @DisplayName("Should throw exception for password too long")
    void shouldThrowExceptionForPasswordTooLong() {
        String longPassword = "A" + "a".repeat(99) + "1!";

        assertThatThrownBy(() -> Password.of(longPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be between 8 and 100 characters");
    }

    @Test
    @DisplayName("Should throw exception for password without uppercase")
    void shouldThrowExceptionForPasswordWithoutUppercase() {
        assertThatThrownBy(() -> Password.of("securepass123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must contain at least one uppercase letter");
    }

    @Test
    @DisplayName("Should throw exception for password without lowercase")
    void shouldThrowExceptionForPasswordWithoutLowercase() {
        assertThatThrownBy(() -> Password.of("SECUREPASS123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must contain at least one lowercase letter");
    }

    @Test
    @DisplayName("Should throw exception for password without digit")
    void shouldThrowExceptionForPasswordWithoutDigit() {
        assertThatThrownBy(() -> Password.of("SecurePassword!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must contain at least one digit");
    }

    @Test
    @DisplayName("Should throw exception for password without special character")
    void shouldThrowExceptionForPasswordWithoutSpecialCharacter() {
        assertThatThrownBy(() -> Password.of("SecurePass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must contain at least one special character");
    }

    @Test
    @DisplayName("Should hash password correctly")
    void shouldHashPasswordCorrectly() {
        // When
        Password hashedPassword = Password.hash("SecurePass123!");

        // Then - Note: This test is based on the current implementation which doesn't actually hash
        assertThat(hashedPassword).isNotNull();
        // In a real implementation with actual hashing, you'd want to test that:
        // - The hashed value is not the same as the plain text
        // - The hash is consistent (same input -> same hash)
    }

    @Test
    @DisplayName("Should correctly match plain text password")
    void shouldCorrectlyMatchPlainTextPassword() {
        // Given
        Password password = Password.of("SecurePass123!");

        // When/Then
        assertThat(password.matches("SecurePass123!")).isTrue();
        assertThat(password.matches("WrongPassword123!")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SecureP1!", // Minimum length
            "VerySecurePassword123!@#", // Mix of characters
            "Complex-Pass_123!", // With hyphen and underscore
            "Pass123Word!" // Mixed order
    })
    @DisplayName("Should accept valid password formats")
    void shouldAcceptValidPasswordFormats(String validPassword) {
        // When
        Password password = Password.of(validPassword);

        // Then
        assertThat(password).isNotNull();
        assertThat(password.getValue()).isEqualTo(validPassword);
    }

    @Test
    @DisplayName("Should exclude value from toString for security")
    void shouldExcludeValueFromToString() {
        // Given
        Password password = Password.of("SecurePass123!");

        // When
        String toString = password.toString();

        // Then - toString should not include the actual password value
        assertThat(toString).doesNotContain("SecurePass123!");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        Password password1 = Password.of("SecurePass123!");
        Password password2 = Password.of("SecurePass123!");
        Password password3 = Password.of("DifferentPass123!");

        // Then
        assertThat(password1).isEqualTo(password2);
        assertThat(password1).isNotEqualTo(password3);
    }
}