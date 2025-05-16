package fr.lpreaux.usermanager.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@EqualsAndHashCode
@ToString
public class Permission {
    private final String name;

    private Permission(String name) {
        Objects.requireNonNull(name, "Permission name cannot be null");
        String trimmedName = name.trim().toUpperCase();

        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Permission name cannot be empty");
        }

        this.name = trimmedName;
    }

    public static Permission of(String name) {
        return new Permission(name);
    }
}