package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.RoleAlreadyExistsException;
import fr.lpreaux.usermanager.application.exception.RoleNotFoundException;
import fr.lpreaux.usermanager.application.port.in.RoleManagementUseCase;
import fr.lpreaux.usermanager.application.port.out.RoleRepository;
import fr.lpreaux.usermanager.domain.model.Permission;
import fr.lpreaux.usermanager.domain.model.Role;
import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de gestion des rôles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleManagementService implements RoleManagementUseCase {

    private final RoleRepository roleRepository;

    @Override
    public RoleId createRole(CreateRoleCommand command) {
        log.info("Creating new role with name: {}", command.name());

        // Vérifier si le rôle existe déjà
        if (roleRepository.existsByName(command.name())) {
            log.warn("Role creation failed: Role already exists with name: {}", command.name());
            throw new RoleAlreadyExistsException("Role already exists with name: " + command.name());
        }

        // Créer le rôle
        Role role = Role.create(command.name(), command.description());

        // Ajouter les permissions
        if (command.permissions() != null) {
            for (String permissionName : command.permissions()) {
                role = role.addPermission(Permission.of(permissionName));
            }
        }

        // Sauvegarder le rôle
        Role savedRole = roleRepository.save(role);
        log.info("Role created successfully with ID: {}", savedRole.getId().getValue());

        return savedRole.getId();
    }

    @Override
    public void updateRole(UpdateRoleCommand command) {
        log.info("Updating role with ID: {}", command.roleId());

        // Récupérer le rôle
        Role role = findRoleOrThrow(command.roleId());

        // Créer un nouveau rôle avec les informations mises à jour
        Role updatedRole = Role.builder()
                .id(role.getId())
                .name(command.name())
                .description(command.description())
                .permissions(role.getPermissions())
                .build();

        // Sauvegarder le rôle
        roleRepository.save(updatedRole);
        log.info("Role updated successfully with ID: {}", command.roleId());
    }

    @Override
    public void addPermission(AddPermissionCommand command) {
        log.info("Adding permission {} to role with ID: {}", command.permission(), command.roleId());

        // Récupérer le rôle
        Role role = findRoleOrThrow(command.roleId());

        // Ajouter la permission
        Permission permission = Permission.of(command.permission());
        Role updatedRole = role.addPermission(permission);

        // Sauvegarder le rôle
        roleRepository.save(updatedRole);
        log.info("Permission added successfully to role with ID: {}", command.roleId());
    }

    @Override
    public void removePermission(RemovePermissionCommand command) {
        log.info("Removing permission {} from role with ID: {}", command.permission(), command.roleId());

        // Récupérer le rôle
        Role role = findRoleOrThrow(command.roleId());

        // Supprimer la permission
        Permission permission = Permission.of(command.permission());
        Role updatedRole = role.removePermission(permission);

        // Sauvegarder le rôle
        roleRepository.save(updatedRole);
        log.info("Permission removed successfully from role with ID: {}", command.roleId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleDTO> findRoleById(String roleId) {
        log.debug("Finding role by ID: {}", roleId);

        return roleRepository.findById(RoleId.of(roleId))
                .map(this::mapToRoleDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleDTO> findRoleByName(String name) {
        log.debug("Finding role by name: {}", name);

        return roleRepository.findByName(name)
                .map(this::mapToRoleDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        log.debug("Getting all roles");

        return roleRepository.findAll().stream()
                .map(this::mapToRoleDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteRole(String roleId) {
        log.info("Deleting role with ID: {}", roleId);

        // Vérifier si le rôle existe
        if (roleRepository.findById(RoleId.of(roleId)).isEmpty()) {
            log.warn("Role deletion failed: Role not found with ID: {}", roleId);
            throw new RoleNotFoundException("Role not found with ID: " + roleId);
        }

        // Supprimer le rôle
        roleRepository.delete(RoleId.of(roleId));
        log.info("Role deleted successfully with ID: {}", roleId);
    }

    private Role findRoleOrThrow(String roleId) {
        return roleRepository.findById(RoleId.of(roleId))
                .orElseThrow(() -> {
                    log.warn("Role not found with ID: {}", roleId);
                    return new RoleNotFoundException("Role not found with ID: " + roleId);
                });
    }

    private RoleDTO mapToRoleDTO(Role role) {
        Set<String> permissions = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return new RoleDTO(
                role.getId().getValue().toString(),
                role.getName(),
                role.getDescription(),
                permissions
        );
    }
}