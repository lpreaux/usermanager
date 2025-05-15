package fr.lpreaux.usermanager.domain.model.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
public class RoleId {
    private final UUID value;

    private RoleId(UUID value) {
        this.value = Objects.requireNonNull(value, "Role ID cannot be null");
    }

    public static RoleId of(UUID value) {
        return new RoleId(value);
    }

    public static RoleId of(String value) {
        return new RoleId(UUID.fromString(value));
    }

    public static RoleId generate() {
        return new RoleId(UUID.randomUUID());
    }
}