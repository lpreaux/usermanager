package fr.lpreaux.usermanager.application.exception;

public class InvalidTokenException extends UserManagementException {
    public InvalidTokenException(String message) {
        super(message);
    }
}