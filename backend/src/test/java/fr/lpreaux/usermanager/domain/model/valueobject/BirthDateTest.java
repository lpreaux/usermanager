package fr.lpreaux.usermanager.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires pour le Value Object BirthDate.
 * Les Value Objects sont immutables et contiennent leur propre logique de validation.
 */
class BirthDateTest {

    @Test
    @DisplayName("Should create BirthDate with valid date")
    void shouldCreateBirthDateWithValidDate() {
        // Given
        LocalDate date = LocalDate.of(1990, 5, 15);

        // When
        BirthDate birthDate = BirthDate.of(date);

        // Then
        assertThat(birthDate).isNotNull();
        assertThat(birthDate.getValue()).isEqualTo(date);
    }

    @Test
    @DisplayName("Should create BirthDate from year, month, day")
    void shouldCreateBirthDateFromYearMonthDay() {
        // When
        BirthDate birthDate = BirthDate.of(1990, 5, 15);

        // Then
        assertThat(birthDate.getValue()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    @Test
    @DisplayName("Should throw exception for null date")
    void shouldThrowExceptionForNullDate() {
        assertThatThrownBy(() -> BirthDate.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Birth date cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for future date")
    void shouldThrowExceptionForFutureDate() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(1);

        // When/Then
        assertThatThrownBy(() -> BirthDate.of(futureDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Birth date cannot be in the future");
    }

    @Test
    @DisplayName("Should correctly determine if person is adult")
    void shouldCorrectlyDetermineIfPersonIsAdult() {
        // Given
        LocalDate adultDate = LocalDate.now().minusYears(20);
        LocalDate minorDate = LocalDate.now().minusYears(16);
        LocalDate exactlyEighteen = LocalDate.now().minusYears(18);

        // When
        BirthDate adultBirthDate = BirthDate.of(adultDate);
        BirthDate minorBirthDate = BirthDate.of(minorDate);
        BirthDate exactlyEighteenBirthDate = BirthDate.of(exactlyEighteen);

        // Then
        assertThat(adultBirthDate.isAdult()).isTrue();
        assertThat(minorBirthDate.isAdult()).isFalse();
        assertThat(exactlyEighteenBirthDate.isAdult()).isTrue();
    }

    @Test
    @DisplayName("Should calculate age correctly")
    void shouldCalculateAgeCorrectly() {
        // Given
        LocalDate birthDate = LocalDate.now().minusYears(25).minusMonths(6);

        // When
        BirthDate birthDateVO = BirthDate.of(birthDate);

        // Then
        assertThat(birthDateVO.getAge()).isEqualTo(25);
    }

    // Ajouter des tests pour BirthDate qui vérifient spécifiquement :
    @Test
    void shouldCalculateAgeCorrectlyWithEdgeCases() {
        // Tester la veille d'un anniversaire
        LocalDate almostBirthday = LocalDate.now().minusYears(30).plusDays(1);
        BirthDate birthDate = BirthDate.of(almostBirthday);
        assertThat(birthDate.getAge()).isEqualTo(29);

        // Tester le jour même d'un anniversaire
        LocalDate exactBirthday = LocalDate.now().minusYears(25);
        BirthDate exactBirthDate = BirthDate.of(exactBirthday);
        assertThat(exactBirthDate.getAge()).isEqualTo(25);
    }
}