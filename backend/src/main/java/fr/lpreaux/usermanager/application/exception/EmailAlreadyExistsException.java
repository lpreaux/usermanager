package fr.lpreaux.usermanager.application.exception;

/**
 * Exception thrown when an email already exists.
 */
public class EmailAlreadyExistsException extends UserManagementException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
