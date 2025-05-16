package fr.lpreaux.usermanager.application.exception;

/**
 * Exception thrown when trying to remove the last email.
 */
public class LastEmailException extends UserManagementException {
    public LastEmailException(String message) {
        super(message);
    }
}
