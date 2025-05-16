package fr.lpreaux.usermanager.application.port.out;

import fr.lpreaux.usermanager.domain.model.User;
import fr.lpreaux.usermanager.domain.model.valueobject.Email;
import fr.lpreaux.usermanager.domain.model.valueobject.Login;
import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;
import fr.lpreaux.usermanager.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for user persistence operations.
 * In hexagonal architecture, this interface defines how the application layer
 * wants to interact with the persistence layer.
 */
public interface UserRepository {

    /**
     * Saves a user.
     * @param user The user to save
     * @return The saved user
     */
    User save(User user);

    /**
     * Finds a user by their identifier.
     * @param userId The user identifier
     * @return An Optional containing the user if it exists, empty otherwise
     */
    Optional<User> findById(UserId userId);

    /**
     * Finds a user by their login.
     * @param login The user's login
     * @return An Optional containing the user if it exists, empty otherwise
     */
    Optional<User> findByLogin(Login login);

    /**
     * Finds a user by their email address.
     * @param email The user's email address
     * @return An Optional containing the user if it exists, empty otherwise
     */
    Optional<User> findByEmail(Email email);

    /**
     * Retrieves all users.
     * @return The list of users
     */
    List<User> findAll();

    /**
     * Deletes a user.
     * @param userId The identifier of the user to delete
     */
    void delete(UserId userId);

    /**
     * Checks if a login is already in use.
     * @param login The login to check
     * @return true if the login is already in use, false otherwise
     */
    boolean existsByLogin(Login login);

    /**
     * Checks if an email address is already in use.
     * @param email The email address to check
     * @return true if the email is already in use, false otherwise
     */
    boolean existsByEmail(Email email);

    /**
     * Trouve les utilisateurs ayant un rôle spécifique.
     *
     * @param roleId L'ID du rôle
     * @return La liste des utilisateurs ayant ce rôle
     */
    List<User> findByRoleId(RoleId roleId);

    /**
     * Vérifie si un utilisateur a un rôle spécifique.
     *
     * @param userId L'ID de l'utilisateur
     * @param roleId L'ID du rôle
     * @return true si l'utilisateur a ce rôle, sinon false
     */
    boolean hasRole(UserId userId, RoleId roleId);
}