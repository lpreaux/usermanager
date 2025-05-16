package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginTest {

    @Test
    @DisplayName("Should create valid login")
    void shouldCreateValidLogin() {
        // When
        Login login = Login.of("john.doe");

        // Then
        assertThat(login).isNotNull();
        assertThat(login.getValue()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("Should trim whitespace from login")
    void shouldTrimWhitespaceFromLogin() {
        // When
        Login login = Login.of("  john.doe  ");

        // Then
        assertThat(login.getValue()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("Should throw exception for null login")
    void shouldThrowExceptionForNullLogin() {
        assertThatThrownBy(() -> Login.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Login cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for empty login")
    void shouldThrowExceptionForEmptyLogin() {
        assertThatThrownBy(() -> Login.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login must be between");
    }

    @Test
    @DisplayName("Should throw exception for login too short")
    void shouldThrowExceptionForLoginTooShort() {
        assertThatThrownBy(() -> Login.of("usr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login must be between 4 and 50 characters");
    }

    @Test
    @DisplayName("Should throw exception for login too long")
    void shouldThrowExceptionForLoginTooLong() {
        String longLogin = "a".repeat(51);

        assertThatThrownBy(() -> Login.of(longLogin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login must be between 4 and 50 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "valid.login",
            "valid_login",
            "valid-login",
            "valid123",
            "123valid"
    })
    @DisplayName("Should accept valid login formats")
    void shouldAcceptValidLoginFormats(String validLogin) {
        // When
        Login login = Login.of(validLogin);

        // Then
        assertThat(login.getValue()).isEqualTo(validLogin);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid@login",
            "invalid#login",
            "invalid$login",
            "invalid login",
            "invalid*login"
    })
    @DisplayName("Should reject invalid login formats")
    void shouldRejectInvalidLoginFormats(String invalidLogin) {
        assertThatThrownBy(() -> Login.of(invalidLogin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Login can only contain letters, numbers, dots, hyphens, and underscores");
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        Login login1 = Login.of("john.doe");
        Login login2 = Login.of("john.doe");
        Login login3 = Login.of("jane.doe");

        // Then
        assertThat(login1).isEqualTo(login2);
        assertThat(login1).isNotEqualTo(login3);
        assertThat(login1.hashCode()).isEqualTo(login2.hashCode());
        assertThat(login1.hashCode()).isNotEqualTo(login3.hashCode());
    }

    @Test
    @DisplayName("Should have proper string representation")
    void shouldHaveProperStringRepresentation() {
        // Given
        Login login = Login.of("john.doe");

        // When
        String toString = login.toString();

        // Then
        assertThat(toString).contains("john.doe");
    }
}