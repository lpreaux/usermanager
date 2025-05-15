package fr.lpreaux.usermanager.application.exception;

public class RoleNotFoundException extends UserManagementException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}