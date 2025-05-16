package fr.lpreaux.usermanager.application.exception;

/**
 * Base exception class for user management operations.
 */
public abstract class UserManagementException extends RuntimeException {

    public UserManagementException(String message) {
        super(message);
    }

    public UserManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}

