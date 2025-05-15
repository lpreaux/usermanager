package fr.lpreaux.usermanager.application.exception;

public class InvalidCredentialsException extends UserManagementException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}