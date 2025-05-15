package fr.lpreaux.usermanager.application.service;

import fr.lpreaux.usermanager.application.exception.RoleNotFoundException;
import fr.lpreaux.usermanager.application.exception.UserNotFoundException;
import fr.lpreaux.usermanager.application.port.in.RoleManagementUseCase;
import fr.lpreaux.usermanager.application.port.in.UserRoleUseCase;
import fr.lpreaux.usermanager.application.port.out.RoleRepository;
import fr.lpreaux.usermanager.application.port.out.UserRepository;
import fr.lpreaux.usermanager.domain.model.Role;
import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;
import fr.lpreaux.usermanager.domain.model.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de gestion des rôles des utilisateurs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserRoleService implements UserRoleUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleManagementUseCase roleManagementUseCase;

    @Override
    public void assignRole(AssignRoleCommand command) {
        log.info("Assigning role {} to user {}", command.roleId(), command.userId());

        // Récupérer l'utilisateur
        User user = findUserOrThrow(command.userId());

        // Récupérer le rôle
        Role role = findRoleOrThrow(command.roleId());

        // Assigner le rôle
        User updatedUser = user.addRole(role);

        // Sauvegarder l'utilisateur
        userRepository.save(updatedUser);
        log.info("Role assigned successfully to user {}", command.userId());
    }

    @Override
    public void removeRole(RemoveRoleCommand command) {
        log.info("Removing role {} from user {}", command.roleId(), command.userId());

        // Récupérer l'utilisateur
        User user = findUserOrThrow(command.userId());

        // Récupérer le rôle
        Role role = findRoleOrThrow(command.roleId());

        // Retirer le rôle
        User updatedUser = user.removeRole(role);

        // Sauvegarder l'utilisateur
        userRepository.save(updatedUser);
        log.info("Role removed successfully from user {}", command.userId());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<RoleManagementUseCase.RoleDTO> getUserRoles(String userId) {
        log.debug("Getting roles for user {}", userId);

        // Récupérer l'utilisateur
        User user = findUserOrThrow(userId);

        // Récupérer les rôles
        return user.getRoles().stream()
                .map(role -> roleManagementUseCase.findRoleById(role.getId().getValue().toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(String userId) {
        log.debug("Getting permissions for user {}", userId);

        // Récupérer l'utilisateur
        User user = findUserOrThrow(userId);

        // Récupérer et combiner les permissions de tous les rôles
        Set<String> permissions = new HashSet<>();
        user.getRoles().forEach(role -> {
            role.getPermissions().forEach(permission -> {
                permissions.add(permission.getName());
            });
        });

        return permissions;
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UserNotFoundException("User not found with ID: " + userId);
                });
    }

    private Role findRoleOrThrow(String roleId) {
        return roleRepository.findById(RoleId.of(roleId))
                .orElseThrow(() -> {
                    log.warn("Role not found with ID: {}", roleId);
                    return new RoleNotFoundException("Role not found with ID: " + roleId);
                });
    }
}