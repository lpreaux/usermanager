package fr.lpreaux.usermanager.application.exception;

/**
 * Exception thrown when an invalid password is provided.
 */
public class InvalidPasswordException extends UserManagementException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
