package fr.lpreaux.usermanager.application.port.out;

import fr.lpreaux.usermanager.domain.model.Role;
import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie pour la persistance des rôles.
 */
public interface RoleRepository {

    /**
     * Sauvegarde un rôle.
     *
     * @param role Le rôle à sauvegarder
     * @return Le rôle sauvegardé
     */
    Role save(Role role);

    /**
     * Recherche un rôle par son ID.
     *
     * @param roleId L'ID du rôle
     * @return Le rôle s'il existe, sinon Optional vide
     */
    Optional<Role> findById(RoleId roleId);

    /**
     * Recherche un rôle par son nom.
     *
     * @param name Le nom du rôle
     * @return Le rôle s'il existe, sinon Optional vide
     */
    Optional<Role> findByName(String name);

    /**
     * Récupère tous les rôles.
     *
     * @return La liste de tous les rôles
     */
    List<Role> findAll();

    /**
     * Supprime un rôle.
     *
     * @param roleId L'ID du rôle à supprimer
     */
    void delete(RoleId roleId);

    /**
     * Vérifie si un rôle existe avec ce nom.
     *
     * @param name Le nom du rôle
     * @return true si un rôle existe avec ce nom, sinon false
     */
    boolean existsByName(String name);
}