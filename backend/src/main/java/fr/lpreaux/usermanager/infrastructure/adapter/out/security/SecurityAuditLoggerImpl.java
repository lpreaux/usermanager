package fr.lpreaux.usermanager.infrastructure.adapter.out.security;

import fr.lpreaux.usermanager.application.port.out.SecurityAuditLogger;
import fr.lpreaux.usermanager.infrastructure.adapter.out.analytics.AnalyticsService;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation du logger d'audit de sécurité.
 * Centralise la journalisation des événements de sécurité vers plusieurs destinations:
 * - Fichiers de logs (via SLF4J)
 * - Sentry pour la surveillance des erreurs
 * - PostHog pour l'analyse des événements
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditLoggerImpl implements SecurityAuditLogger {

    private final AnalyticsService analyticsService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void logSecurityEvent(String eventType, String userId, String ipAddress, boolean success, Map<String, Object> details) {
        // 1. Création du message structuré pour les logs
        StringBuilder logMessage = buildLogMessage(eventType, userId, ipAddress, success, details);

        // 2. Journalisation avec le niveau approprié
        logToAppropriateLevel(eventType, success, logMessage.toString());

        // 3. Envoi à Sentry si c'est un événement d'échec ou critique
        if (!success || isCriticalEvent(eventType)) {
            sendToSentry(eventType, userId, ipAddress, success, details);
        }

        // 4. Envoi à l'analytique
        sendToAnalytics(eventType, userId, ipAddress, success, details);
    }

    private StringBuilder buildLogMessage(String eventType, String userId, String ipAddress, boolean success, Map<String, Object> details) {
        StringBuilder message = new StringBuilder();
        message.append("Security event: ").append(eventType);
        message.append(", User: ").append(userId != null && !userId.equals("unknown") ? userId : "anonymous");
        message.append(", IP: ").append(ipAddress);
        message.append(", Status: ").append(success ? "SUCCESS" : "FAILURE");
        message.append(", Time: ").append(LocalDateTime.now().format(FORMATTER));

        if (details != null && !details.isEmpty()) {
            message.append(", Details: ");
            details.forEach((key, value) -> message.append(key).append("=").append(value).append(", "));
            // Supprimer la dernière virgule et espace
            message.delete(message.length() - 2, message.length());
        }

        return message;
    }

    private void logToAppropriateLevel(String eventType, boolean success, String message) {
        if (success) {
            // Succès normal - INFO
            log.info(message);
        } else if (isCriticalEvent(eventType)) {
            // Échec critique - ERROR
            log.error(message);
        } else {
            // Échec non critique - WARN
            log.warn(message);
        }
    }

    private boolean isCriticalEvent(String eventType) {
        return eventType.contains("_blocked") ||
                eventType.contains("brute_force") ||
                eventType.contains("privilege_escalation") ||
                eventType.contains("suspicious") ||
                eventType.contains("violation");
    }

    private void sendToSentry(String eventType, String userId, String ipAddress, boolean success, Map<String, Object> details) {
        try {
            SentryLevel level = success ? SentryLevel.INFO :
                    isCriticalEvent(eventType) ? SentryLevel.ERROR : SentryLevel.WARNING;

            Sentry.withScope(scope -> {
                scope.setLevel(level);
                scope.setTag("event_type", eventType);
                scope.setTag("security_event", "true");
                scope.setTag("status", success ? "success" : "failure");

                if (userId != null && !userId.equals("unknown")) {
                    User user = new User();
                    user.setId(userId);
                    scope.setUser(user);
                }

                scope.setExtra("ip_address", ipAddress);

                // Ajouter tous les détails comme extras
                if (details != null) {
                    details.forEach((s, s1) -> scope.setExtra(s, s1.toString()));
                }

                // Ajouter des breadcrumbs pour avoir plus de contexte
                Sentry.addBreadcrumb("Security event: " + eventType + " (" + (success ? "success" : "failure") + ")");

                // Capturer un message plutôt qu'une exception
                Sentry.captureMessage("Security: " + eventType);
            });
        } catch (Exception e) {
            // Ne pas laisser une erreur dans Sentry perturber le flux d'application
            log.error("Failed to send security event to Sentry", e);
        }
    }

    private void sendToAnalytics(String eventType, String userId, String ipAddress, boolean success, Map<String, Object> details) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("event_type", eventType);
            eventData.put("user_id", userId != null && !userId.equals("unknown") ? userId : "anonymous");
            eventData.put("ip_address", ipAddress);
            eventData.put("success", success);
            eventData.put("timestamp", Instant.now().toEpochMilli());

            // Ajouter tous les détails
            if (details != null) {
                eventData.putAll(details);
            }

            // Éviter d'inclure des données sensibles en production
            sanitizeForAnalytics(eventData);

            // Envoyer l'événement
            analyticsService.trackEvent(
                    userId != null && !userId.equals("unknown") ? userId : "anonymous",
                    "security_" + eventType,
                    eventData
            );
        } catch (Exception e) {
            // Ne pas laisser une erreur dans l'analytique perturber le flux d'application
            log.error("Failed to send security event to analytics", e);
        }
    }

    private void sanitizeForAnalytics(Map<String, Object> data) {
        // Masquer les données potentiellement sensibles
        if (data.containsKey("password")) {
            data.put("password", "********");
        }

        if (data.containsKey("token")) {
            String token = data.get("token").toString();
            if (token.length() > 10) {
                data.put("token", token.substring(0, 5) + "..." + token.substring(token.length() - 5));
            } else {
                data.put("token", "***");
            }
        }

        // Ne pas inclure les détails d'erreur complets en production
        if ("production".equals(System.getProperty("spring.profiles.active"))) {
            if (data.containsKey("error_message")) {
                data.put("error_message", "Error details hidden in production");
            }

            if (data.containsKey("stack_trace")) {
                data.remove("stack_trace");
            }
        }
    }
}