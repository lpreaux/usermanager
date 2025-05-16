package fr.lpreaux.usermanager.domain.model;

import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;
import lombok.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Role {
    private final RoleId id;
    private final String name;
    private final String description;
    private final Set<Permission> permissions;

    public static Role create(String name, String description) {
        return Role.builder()
                .id(RoleId.generate())
                .name(name)
                .description(description)
                .permissions(new HashSet<>())
                .build();
    }

    public Role addPermission(Permission permission) {
        if (permission == null) {
            return this;
        }
        Set<Permission> newPermissions = new HashSet<>(this.permissions);
        newPermissions.add(permission);
        return this.toBuilder()
                .permissions(Collections.unmodifiableSet(newPermissions))
                .build();
    }

    public Role removePermission(Permission permission) {
        if (permission == null) {
            return this;
        }
        Set<Permission> newPermissions = new HashSet<>(this.permissions);
        newPermissions.remove(permission);
        return this.toBuilder()
                .permissions(Collections.unmodifiableSet(newPermissions))
                .build();
    }

    public boolean hasPermission(Permission permission) {
        return this.permissions.contains(permission);
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }
}