package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing an email address.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Email {
    private static final int MAX_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private final String value;

    private Email(String value) {
        Objects.requireNonNull(value, "Email cannot be null");
        value = value.trim().toLowerCase();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Email cannot exceed %d characters", MAX_LENGTH));
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        this.value = value;
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
