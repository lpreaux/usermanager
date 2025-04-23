package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object representing a user's birth date.
 */
@Getter
@EqualsAndHashCode
@ToString
public class BirthDate {
    private final LocalDate value;

    private BirthDate(LocalDate value) {
        Objects.requireNonNull(value, "Birth date cannot be null");

        if (value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }

        this.value = value;
    }

    public static BirthDate of(LocalDate value) {
        return new BirthDate(value);
    }

    public static BirthDate of(int year, int month, int day) {
        return new BirthDate(LocalDate.of(year, month, day));
    }

    public boolean isAdult() {
        return this.value.plusYears(18).isBefore(LocalDate.now()) ||
                this.value.plusYears(18).isEqual(LocalDate.now());
    }

    public int getAge() {
        return LocalDate.now().getYear() - this.value.getYear();
    }
}