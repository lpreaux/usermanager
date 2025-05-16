package fr.lpreaux.usermanager.application.exception;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends UserManagementException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
