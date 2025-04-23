package fr.lpreaux.usermanager.application.port.in;

import fr.lpreaux.usermanager.application.exception.UserAlreadyExistsException;
import fr.lpreaux.usermanager.domain.model.valueobject.UserId;

import java.time.LocalDate;
import java.util.List;

/**
 * Input port for user registration use case.
 */
public interface RegisterUserUseCase {

    /**
     * Command for user registration with all required information.
     */
    record RegisterUserCommand(
            String login,
            String password,
            String lastName,
            String firstName,
            LocalDate birthDate,
            List<String> emails,
            List<String> phoneNumbers
    ) {}

    /**
     * Registers a new user in the system.
     *
     * @param command The registration command with user details
     * @return The ID of the newly created user
     * @throws UserAlreadyExistsException If a user with the same login or email already exists
     */
    UserId registerUser(RegisterUserCommand command) throws UserAlreadyExistsException;
}