package fr.lpreaux.usermanager.application.port.in;

import java.util.Set;

/**
 * Port d'entrée pour gérer les rôles des utilisateurs.
 */
public interface UserRoleUseCase {

    /**
     * Command pour assigner un rôle à un utilisateur.
     */
    record AssignRoleCommand(
            String userId,
            String roleId
    ) {}

    /**
     * Command pour retirer un rôle à un utilisateur.
     */
    record RemoveRoleCommand(
            String userId,
            String roleId
    ) {}

    /**
     * Assigne un rôle à un utilisateur.
     *
     * @param command L'ID de l'utilisateur et l'ID du rôle à assigner
     * @throws fr.lpreaux.usermanager.application.exception.UserNotFoundException Si l'utilisateur n'existe pas
     * @throws fr.lpreaux.usermanager.application.exception.RoleNotFoundException Si le rôle n'existe pas
     */
    void assignRole(AssignRoleCommand command);

    /**
     * Retire un rôle à un utilisateur.
     *
     * @param command L'ID de l'utilisateur et l'ID du rôle à retirer
     * @throws fr.lpreaux.usermanager.application.exception.UserNotFoundException Si l'utilisateur n'existe pas
     * @throws fr.lpreaux.usermanager.application.exception.RoleNotFoundException Si le rôle n'existe pas
     */
    void removeRole(RemoveRoleCommand command);

    /**
     * Récupère tous les rôles d'un utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @return L'ensemble des rôles de l'utilisateur
     * @throws fr.lpreaux.usermanager.application.exception.UserNotFoundException Si l'utilisateur n'existe pas
     */
    Set<RoleManagementUseCase.RoleDTO> getUserRoles(String userId);

    /**
     * Récupère toutes les permissions d'un utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @return L'ensemble des permissions de l'utilisateur
     * @throws fr.lpreaux.usermanager.application.exception.UserNotFoundException Si l'utilisateur n'existe pas
     */
    Set<String> getUserPermissions(String userId);
}