package fr.lpreaux.usermanager.application.port.in;

import fr.lpreaux.usermanager.application.exception.UserNotFoundException;

/**
 * Input port for user deletion use case.
 */
public interface DeleteUserUseCase {

    /**
     * Deletes a user from the system.
     *
     * @param userId The ID of the user to delete
     * @throws UserNotFoundException If the user does not exist
     */
    void deleteUser(String userId) throws UserNotFoundException;
}