package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Value Object representing a user's first name.
 */
@Getter
@EqualsAndHashCode
@ToString
public class FirstName {
    private static final int MAX_LENGTH = 100;

    private final String value;

    private FirstName(String value) {
        Objects.requireNonNull(value, "First name cannot be null");
        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("First name cannot exceed %d characters", MAX_LENGTH));
        }

        this.value = value;
    }

    public static FirstName of(String value) {
        return new FirstName(value);
    }
}