package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a user's password.
 */
@Getter
@EqualsAndHashCode
@ToString(exclude = "value")
public class Password {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;
    private static final Pattern HAS_UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[^a-zA-Z0-9].*");

    private final String value;

    private Password(String value) {
        Objects.requireNonNull(value, "Password cannot be null");

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Password must be between %d and %d characters", MIN_LENGTH, MAX_LENGTH));
        }

        if (!HAS_UPPERCASE.matcher(value).matches()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!HAS_LOWERCASE.matcher(value).matches()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        if (!HAS_DIGIT.matcher(value).matches()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        if (!HAS_SPECIAL.matcher(value).matches()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }

        this.value = value;
    }

    public static Password of(String value) {
        return new Password(value);
    }

    /**
     * Factory method to create a hashed password (to be implemented with a real hashing algorithm)
     */
    public static Password hash(String plainTextPassword) {
        // In a real case, we would hash the password here
        // For now, we just use validation
        return new Password(plainTextPassword);
    }

    /**
     * Checks if a plain text password matches this hashed password
     */
    public boolean matches(String plainTextPassword) {
        // In a real case, we would compare the hash
        // For now, we do a simple comparison
        return this.value.equals(plainTextPassword);
    }
}