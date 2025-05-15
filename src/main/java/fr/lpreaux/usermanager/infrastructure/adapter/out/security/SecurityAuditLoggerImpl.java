package fr.lpreaux.usermanager.infrastructure.adapter.out.security;

import fr.lpreaux.usermanager.application.port.out.SecurityAuditLogger;
import fr.lpreaux.usermanager.infrastructure.adapter.out.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditLoggerImpl implements SecurityAuditLogger {

    private final AnalyticsService analyticsService; // Votre service d'analytics existant

    @Override
    public void logSecurityEvent(String eventType, String userId, String ipAddress, boolean success, Map<String, Object> details) {
        // Créer un message structuré pour la journalisation
        StringBuilder message = new StringBuilder();
        message.append("Security event: ").append(eventType);
        message.append(", User: ").append(userId != null ? userId : "anonymous");
        message.append(", IP: ").append(ipAddress);
        message.append(", Status: ").append(success ? "SUCCESS" : "FAILURE");

        if (details != null && !details.isEmpty()) {
            message.append(", Details: ").append(details);
        }

        // Journaliser avec le niveau approprié
        if (success) {
            log.info(message.toString());
        } else {
            log.warn(message.toString());
        }

        // Envoyer l'événement à l'analytique
        Map<String, Object> eventData = Map.of(
                "event_type", "security_" + eventType,
                "user_id", userId != null ? userId : "anonymous",
                "ip_address", ipAddress,
                "success", success,
                "timestamp", LocalDateTime.now().toString(),
                "details", details != null ? details : java.util.Map.of()
        );

        analyticsService.trackEvent(userId != null ? userId : "anonymous",
                "security_event", eventData);
    }
}