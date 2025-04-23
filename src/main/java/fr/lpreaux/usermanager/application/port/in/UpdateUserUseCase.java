package fr.lpreaux.usermanager.application.port.in;

import fr.lpreaux.usermanager.application.exception.EmailAlreadyExistsException;
import fr.lpreaux.usermanager.application.exception.InvalidPasswordException;
import fr.lpreaux.usermanager.application.exception.LastEmailException;
import fr.lpreaux.usermanager.application.exception.UserNotFoundException;

import java.time.LocalDate;

/**
 * Input port for user update use cases.
 */
public interface UpdateUserUseCase {

    /**
     * Command for updating a user's personal information.
     */
    record UpdatePersonalInfoCommand(
            String userId,
            String lastName,
            String firstName,
            LocalDate birthDate
    ) {}

    /**
     * Command for changing a user's password.
     */
    record ChangePasswordCommand(
            String userId,
            String currentPassword,
            String newPassword
    ) {}

    /**
     * Command for adding an email to a user.
     */
    record AddEmailCommand(
            String userId,
            String email
    ) {}

    /**
     * Command for removing an email from a user.
     */
    record RemoveEmailCommand(
            String userId,
            String email
    ) {}

    /**
     * Command for adding a phone number to a user.
     */
    record AddPhoneNumberCommand(
            String userId,
            String phoneNumber
    ) {}

    /**
     * Command for removing a phone number from a user.
     */
    record RemovePhoneNumberCommand(
            String userId,
            String phoneNumber
    ) {}

    /**
     * Updates a user's personal information.
     *
     * @param command The update command with user details
     * @throws UserNotFoundException If the user does not exist
     */
    void updatePersonalInfo(UpdatePersonalInfoCommand command) throws UserNotFoundException;

    /**
     * Changes a user's password.
     *
     * @param command The password change command
     * @throws UserNotFoundException If the user does not exist
     * @throws InvalidPasswordException If the current password is incorrect
     */
    void changePassword(ChangePasswordCommand command) throws UserNotFoundException, InvalidPasswordException;

    /**
     * Adds an email to a user.
     *
     * @param command The add email command
     * @throws UserNotFoundException If the user does not exist
     * @throws EmailAlreadyExistsException If the email is already in use
     */
    void addEmail(AddEmailCommand command) throws UserNotFoundException, EmailAlreadyExistsException;

    /**
     * Removes an email from a user.
     *
     * @param command The remove email command
     * @throws UserNotFoundException If the user does not exist
     * @throws LastEmailException If this is the user's last email
     */
    void removeEmail(RemoveEmailCommand command) throws UserNotFoundException, LastEmailException;

    /**
     * Adds a phone number to a user.
     *
     * @param command The add phone number command
     * @throws UserNotFoundException If the user does not exist
     */
    void addPhoneNumber(AddPhoneNumberCommand command) throws UserNotFoundException;

    /**
     * Removes a phone number from a user.
     *
     * @param command The remove phone number command
     * @throws UserNotFoundException If the user does not exist
     */
    void removePhoneNumber(RemovePhoneNumberCommand command) throws UserNotFoundException;
}