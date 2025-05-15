package fr.lpreaux.usermanager.application.port.in;

import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Port d'entrée pour la gestion des rôles.
 */
public interface RoleManagementUseCase {

    /**
     * Command pour créer un rôle.
     */
    record CreateRoleCommand(
            String name,
            String description,
            Set<String> permissions
    ) {}

    /**
     * Command pour mettre à jour un rôle.
     */
    record UpdateRoleCommand(
            String roleId,
            String name,
            String description
    ) {}

    /**
     * Command pour ajouter une permission à un rôle.
     */
    record AddPermissionCommand(
            String roleId,
            String permission
    ) {}

    /**
     * Command pour supprimer une permission d'un rôle.
     */
    record RemovePermissionCommand(
            String roleId,
            String permission
    ) {}

    /**
     * DTO pour les informations d'un rôle.
     */
    record RoleDTO(
            String id,
            String name,
            String description,
            Set<String> permissions
    ) {}

    /**
     * Crée un nouveau rôle.
     *
     * @param command Les informations du rôle à créer
     * @return L'ID du rôle créé
     * @throws fr.lpreaux.usermanager.application.exception.RoleAlreadyExistsException Si un rôle avec le même nom existe déjà
     */
    RoleId createRole(CreateRoleCommand command);

    /**
     * Met à jour un rôle existant.
     *
     * @param command Les informations à jour du rôle
     * @throws fr.lpreaux.usermanager.application.exception.RoleNotFoundException Si le rôle n'existe pas
     */
    void updateRole(UpdateRoleCommand command);

    /**
     * Ajoute une permission à un rôle.
     *
     * @param command L'ID du rôle et la permission à ajouter
     * @throws fr.lpreaux.usermanager.application.exception.RoleNotFoundException Si le rôle n'existe pas
     */
    void addPermission(AddPermissionCommand command);

    /**
     * Supprime une permission d'un rôle.
     *
     * @param command L'ID du rôle et la permission à supprimer
     * @throws fr.lpreaux.usermanager.application.exception.RoleNotFoundException Si le rôle n'existe pas
     */
    void removePermission(RemovePermissionCommand command);

    /**
     * Recherche un rôle par son ID.
     *
     * @param roleId L'ID du rôle à rechercher
     * @return Le rôle s'il existe, sinon Optional vide
     */
    Optional<RoleDTO> findRoleById(String roleId);

    /**
     * Recherche un rôle par son nom.
     *
     * @param name Le nom du rôle à rechercher
     * @return Le rôle s'il existe, sinon Optional vide
     */
    Optional<RoleDTO> findRoleByName(String name);

    /**
     * Récupère tous les rôles.
     *
     * @return La liste de tous les rôles
     */
    List<RoleDTO> getAllRoles();

    /**
     * Supprime un rôle.
     *
     * @param roleId L'ID du rôle à supprimer
     * @throws fr.lpreaux.usermanager.application.exception.RoleNotFoundException Si le rôle n'existe pas
     */
    void deleteRole(String roleId);
}