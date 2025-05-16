package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Value Object representing a user's last name.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Name {
    private static final int MAX_LENGTH = 100;

    private final String value;

    private Name(String value) {
        Objects.requireNonNull(value, "Last name cannot be null");
        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Last name cannot exceed %d characters", MAX_LENGTH));
        }

        this.value = value;
    }

    public static Name of(String value) {
        return new Name(value);
    }
}