package fr.lpreaux.usermanager.application.port.out;

import java.util.Map;

public interface SecurityAuditLogger {
    void logSecurityEvent(String eventType, String userId, String ipAddress, boolean success, Map<String, Object> details);
}
