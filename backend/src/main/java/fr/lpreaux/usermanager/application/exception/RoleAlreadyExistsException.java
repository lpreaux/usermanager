package fr.lpreaux.usermanager.application.exception;

public class RoleAlreadyExistsException extends UserManagementException {
    public RoleAlreadyExistsException(String message) {
        super(message);
    }
}