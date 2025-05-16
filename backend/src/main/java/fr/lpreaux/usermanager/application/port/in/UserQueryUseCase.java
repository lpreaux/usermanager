package fr.lpreaux.usermanager.application.port.in;

import java.util.List;
import java.util.Optional;

/**
 * Input port for user query use cases.
 */
public interface UserQueryUseCase {

    /**
     * DTO for user details returned by queries.
     */
    record UserDetailsDTO(
            String id,
            String login,
            String lastName,
            String firstName,
            String birthDate,
            int age,
            boolean isAdult,
            List<String> emails,
            List<String> phoneNumbers
    ) {}

    /**
     * Finds a user by their ID.
     *
     * @param userId The user ID to search for
     * @return Optional containing user details if found, empty otherwise
     */
    Optional<UserDetailsDTO> findUserById(String userId);

    /**
     * Finds a user by their login.
     *
     * @param login The login to search for
     * @return Optional containing user details if found, empty otherwise
     */
    Optional<UserDetailsDTO> findUserByLogin(String login);

    /**
     * Finds a user by their email.
     *
     * @param email The email to search for
     * @return Optional containing user details if found, empty otherwise
     */
    Optional<UserDetailsDTO> findUserByEmail(String email);

    /**
     * Gets all users in the system.
     *
     * @return List of user details
     */
    List<UserDetailsDTO> getAllUsers();
}