package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    @DisplayName("Should create valid phone number")
    void shouldCreateValidPhoneNumber() {
        // When
        PhoneNumber phoneNumber = PhoneNumber.of("+33612345678");

        // Then
        assertThat(phoneNumber).isNotNull();
        assertThat(phoneNumber.getValue()).isEqualTo("+33612345678");
    }

    @Test
    @DisplayName("Should normalize phone number")
    void shouldNormalizePhoneNumber() {
        // When
        PhoneNumber phoneNumber = PhoneNumber.of("+33 6 12 34 56 78");

        // Then
        assertThat(phoneNumber.getValue()).isEqualTo("+33612345678");
    }

    @Test
    @DisplayName("Should format phone number")
    void shouldFormatPhoneNumber() {
        // Given
        PhoneNumber phoneNumber = PhoneNumber.of("+33612345678");

        // When
        String formatted = phoneNumber.getFormatted();

        // Then
        assertThat(formatted).isEqualTo("+33612345678");
    }

    @Test
    @DisplayName("Should throw exception for null phone number")
    void shouldThrowExceptionForNullPhoneNumber() {
        assertThatThrownBy(() -> PhoneNumber.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Phone number cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for empty phone number")
    void shouldThrowExceptionForEmptyPhoneNumber() {
        assertThatThrownBy(() -> PhoneNumber.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception for too long phone number")
    void shouldThrowExceptionForTooLongPhoneNumber() {
        String longNumber = "+1" + "2".repeat(20);
        assertThatThrownBy(() -> PhoneNumber.of(longNumber))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number cannot exceed 20 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456",  // Valid short format
            "+1-555-555-5555",  // Valid with dashes
            "06 12 34 56 78",  // Valid with spaces
            "+33 612345678"  // Valid international
    })
    @DisplayName("Should accept valid phone formats")
    void shouldAcceptValidPhoneFormats(String validPhone) {
        // When
        PhoneNumber phoneNumber = PhoneNumber.of(validPhone);

        // Then
        assertThat(phoneNumber).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345",  // Too short
            "abcdefghij",  // Letters
            "+33-abc-def-ghij",  // Mixed letters
            "++33612345678"  // Invalid format
    })
    @DisplayName("Should reject invalid phone formats")
    void shouldRejectInvalidPhoneFormats(String invalidPhone) {
        assertThatThrownBy(() -> PhoneNumber.of(invalidPhone))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid phone number format");
    }
}