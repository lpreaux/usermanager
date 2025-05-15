package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirstNameTest {

    @Test
    @DisplayName("Should create valid first name")
    void shouldCreateValidFirstName() {
        // When
        FirstName firstName = FirstName.of("John");

        // Then
        assertThat(firstName).isNotNull();
        assertThat(firstName.getValue()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should trim whitespace from first name")
    void shouldTrimWhitespaceFromFirstName() {
        // When
        FirstName firstName = FirstName.of("  John  ");

        // Then
        assertThat(firstName.getValue()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should throw exception for null first name")
    void shouldThrowExceptionForNullFirstName() {
        assertThatThrownBy(() -> FirstName.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("First name cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for empty first name")
    void shouldThrowExceptionForEmptyFirstName() {
        assertThatThrownBy(() -> FirstName.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First name cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception for first name exceeding max length")
    void shouldThrowExceptionForFirstNameExceedingMaxLength() {
        // Given
        String longFirstName = "a".repeat(101);

        // When/Then
        assertThatThrownBy(() -> FirstName.of(longFirstName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First name cannot exceed 100 characters");
    }

    @Test
    @DisplayName("Should accept first name with maximum allowed length")
    void shouldAcceptFirstNameWithMaximumAllowedLength() {
        // Given
        String maxLengthFirstName = "a".repeat(100);

        // When
        FirstName firstName = FirstName.of(maxLengthFirstName);

        // Then
        assertThat(firstName.getValue()).isEqualTo(maxLengthFirstName);
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        FirstName firstName1 = FirstName.of("John");
        FirstName firstName2 = FirstName.of("John");
        FirstName firstName3 = FirstName.of("Jane");

        // Then
        assertThat(firstName1).isEqualTo(firstName2);
        assertThat(firstName1).isNotEqualTo(firstName3);
        assertThat(firstName1.hashCode()).isEqualTo(firstName2.hashCode());
        assertThat(firstName1.hashCode()).isNotEqualTo(firstName3.hashCode());
    }

    @Test
    @DisplayName("Should have proper string representation")
    void shouldHaveProperStringRepresentation() {
        // Given
        FirstName firstName = FirstName.of("John");

        // When
        String toString = firstName.toString();

        // Then
        assertThat(toString).contains("John");
    }
}