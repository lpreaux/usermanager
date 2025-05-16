package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a user's login.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Login {
    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 50;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final String value;

    private Login(String value) {
        Objects.requireNonNull(value, "Login cannot be null");
        value = value.trim();

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Login must be between %d and %d characters", MIN_LENGTH, MAX_LENGTH));
        }

        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Login can only contain letters, numbers, dots, hyphens, and underscores");
        }

        this.value = value;
    }

    public static Login of(String value) {
        return new Login(value);
    }
}