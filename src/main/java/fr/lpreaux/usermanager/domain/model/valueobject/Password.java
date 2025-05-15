package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private final String value;
    private final boolean isHashed;

    private Password(String value, boolean isHashed) {
        Objects.requireNonNull(value, "Password cannot be null");

        if (!isHashed) {
            // Validation uniquement pour les mots de passe non hachés
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
        }

        this.value = value;
        this.isHashed = isHashed;
    }

    public static Password of(String value) {
        return new Password(value, false);
    }

    public static Password ofHashed(String hashedValue) {
        return new Password(hashedValue, true);
    }

    /**
     * Factory method to create a hashed password.
     */
    public static Password hash(String plainTextPassword) {
        return new Password(PASSWORD_ENCODER.encode(plainTextPassword), true);
    }

    /**
     * Checks if a plain text password matches this hashed password.
     */
    public boolean matches(String plainTextPassword) {
        if (!isHashed) {
            return this.value.equals(plainTextPassword);
        }
        return PASSWORD_ENCODER.matches(plainTextPassword, this.value);
    }
}