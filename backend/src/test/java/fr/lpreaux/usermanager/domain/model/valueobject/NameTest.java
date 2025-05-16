package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NameTest {

    @Test
    @DisplayName("Should create valid name")
    void shouldCreateValidName() {
        // When
        Name name = Name.of("Doe");

        // Then
        assertThat(name).isNotNull();
        assertThat(name.getValue()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should trim whitespace from name")
    void shouldTrimWhitespaceFromName() {
        // When
        Name name = Name.of("  Doe  ");

        // Then
        assertThat(name.getValue()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should throw exception for null name")
    void shouldThrowExceptionForNullName() {
        assertThatThrownBy(() -> Name.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Last name cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for empty name")
    void shouldThrowExceptionForEmptyName() {
        assertThatThrownBy(() -> Name.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Last name cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception for name exceeding max length")
    void shouldThrowExceptionForNameExceedingMaxLength() {
        // Given
        String longName = "a".repeat(101);

        // When/Then
        assertThatThrownBy(() -> Name.of(longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Last name cannot exceed 100 characters");
    }

    @Test
    @DisplayName("Should accept name with maximum allowed length")
    void shouldAcceptNameWithMaximumAllowedLength() {
        // Given
        String maxLengthName = "a".repeat(100);

        // When
        Name name = Name.of(maxLengthName);

        // Then
        assertThat(name.getValue()).isEqualTo(maxLengthName);
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        Name name1 = Name.of("Doe");
        Name name2 = Name.of("Doe");
        Name name3 = Name.of("Smith");

        // Then
        assertThat(name1).isEqualTo(name2);
        assertThat(name1).isNotEqualTo(name3);
        assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
        assertThat(name1.hashCode()).isNotEqualTo(name3.hashCode());
    }

    @Test
    @DisplayName("Should have proper string representation")
    void shouldHaveProperStringRepresentation() {
        // Given
        Name name = Name.of("Doe");

        // When
        String toString = name.toString();

        // Then
        assertThat(toString).contains("Doe");
    }
}