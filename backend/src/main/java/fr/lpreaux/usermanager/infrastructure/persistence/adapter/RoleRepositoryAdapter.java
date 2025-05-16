package fr.lpreaux.usermanager.infrastructure.persistence.adapter;

import fr.lpreaux.usermanager.application.port.out.RoleRepository;
import fr.lpreaux.usermanager.domain.model.Permission;
import fr.lpreaux.usermanager.domain.model.Role;
import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;
import fr.lpreaux.usermanager.infrastructure.persistence.entity.RoleEntity;
import fr.lpreaux.usermanager.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptateur pour le repository de rôles.
 */
@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository roleJpaRepository;

    @Override
    public Role save(Role role) {
        RoleEntity entity = mapToEntity(role);
        RoleEntity savedEntity = roleJpaRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<Role> findById(RoleId roleId) {
        return roleJpaRepository.findById(roleId.getValue())
                .map(this::mapToDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roleJpaRepository.findByName(name)
                .map(this::mapToDomain);
    }

    @Override
    public List<Role> findAll() {
        return roleJpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(RoleId roleId) {
        roleJpaRepository.deleteById(roleId.getValue());
    }

    @Override
    public boolean existsByName(String name) {
        return roleJpaRepository.existsByName(name);
    }

    private RoleEntity mapToEntity(Role role) {
        Set<String> permissionStrings = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return RoleEntity.builder()
                .id(role.getId().getValue())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionStrings)
                .build();
    }

    private Role mapToDomain(RoleEntity entity) {
        Set<Permission> permissions = entity.getPermissions().stream()
                .map(Permission::of)
                .collect(Collectors.toSet());

        return Role.builder()
                .id(RoleId.of(entity.getId()))
                .name(entity.getName())
                .description(entity.getDescription())
                .permissions(permissions)
                .build();
    }
}