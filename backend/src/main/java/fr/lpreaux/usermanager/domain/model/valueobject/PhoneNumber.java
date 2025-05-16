package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a phone number.
 */
@Getter
@EqualsAndHashCode
@ToString
public class PhoneNumber {
    private static final int MAX_LENGTH = 20;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s-]{6,20}$");

    private final String value;

    private PhoneNumber(String value) {
        Objects.requireNonNull(value, "Phone number cannot be null");
        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Phone number cannot exceed %d characters", MAX_LENGTH));
        }

        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // Normalize the number (remove spaces and hyphens)
        this.value = value.replaceAll("[\\s-]", "");
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    public String getFormatted() {
        // Implement formatting as needed
        return this.value;
    }
}