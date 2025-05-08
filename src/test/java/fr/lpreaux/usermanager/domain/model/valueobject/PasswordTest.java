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
        Password password = Password.of("ValidPass123!");

        // Then
        assertThat(password).isNotNull();
        assertThat(password.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Should hash password")
    void shouldHashPassword() {
        // When
        Password password = Password.hash("ValidPass123!");

        // Then
        assertThat(password).isNotNull();
        assertThat(password.getValue()).isEqualTo("ValidPass123!"); // Temporary implementation
    }

    @Test
    @DisplayName("Should match correct password")
    void shouldMatchCorrectPassword() {
        // Given
        Password password = Password.of("ValidPass123!");

        // When/Then
        assertThat(password.matches("ValidPass123!")).isTrue();
        assertThat(password.matches("WrongPassword123!")).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for null password")
    void shouldThrowExceptionForNullPassword() {
        assertThatThrownBy(() -> Password.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Password cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for short password")
    void shouldThrowExceptionForShortPassword() {
        assertThatThrownBy(() -> Password.of("Short1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be between 8 and 100 characters");
    }

    @Test
    @DisplayName("Should throw exception for long password")
    void shouldThrowExceptionForLongPassword() {
        String longPassword = "a".repeat(101) + "A1!";
        assertThatThrownBy(() -> Password.of(longPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be between 8 and 100 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "nouppercasehere1!",
            "NOLOWERCASEHERE1!",
            "NoDigitsHere!",
            "NoSpecialChars123"
    })
    @DisplayName("Should reject invalid password formats")
    void shouldRejectInvalidPasswordFormats(String invalidPassword) {
        assertThatThrownBy(() -> Password.of(invalidPassword))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should implement toString without exposing password")
    void shouldImplementToStringWithoutExposingPassword() {
        // Given
        Password password = Password.of("ValidPass123!");

        // When
        String toString = password.toString();

        // Then
        assertThat(toString).doesNotContain("ValidPass123!");
    }
}