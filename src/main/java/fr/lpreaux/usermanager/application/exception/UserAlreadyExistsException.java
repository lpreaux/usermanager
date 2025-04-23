package fr.lpreaux.usermanager.application.exception;

/**
 * Exception thrown when a user already exists.
 */
public class UserAlreadyExistsException extends UserManagementException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
