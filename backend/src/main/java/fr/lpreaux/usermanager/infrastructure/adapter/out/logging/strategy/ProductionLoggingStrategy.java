package fr.lpreaux.usermanager.infrastructure.adapter.out.logging.strategy;

import java.util.Map;

import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

/**
 * Stratégie de logging pour l'environnement de production.
 * Économe en ressources et centrée sur les informations essentielles.
 */
@Slf4j
public class ProductionLoggingStrategy implements LoggingStrategy {

    @Override
    public boolean shouldLog(String level, String context) {
        // En production, on filtre plus agressivement
        if ("DEBUG".equals(level)) {
            return false; // Pas de DEBUG en prod
        }
        if ("INFO".equals(level) && !isBusinessCriticalContext(context)) {
            return false; // INFO seulement pour le contexte business critique
        }
        return true;
    }

    @Override
    public String formatMessage(String level, String message, String context) {
        // Format concis pour la production
        return String.format("[PROD][%s] %s", level, message);
    }

    @Override
    public void enrichContext(Map<String, String> additionalContext) {
        if (additionalContext != null) {
            // Filtrer pour ne garder que les contextes essentiels
            additionalContext.entrySet().stream()
                    .filter(e -> isEssentialContext(e.getKey()))
                    .forEach(e -> MDC.put(e.getKey(), e.getValue()));
        }
        MDC.put("env", "prod");
    }

    @Override
    public void clearContext() {
        MDC.clear();
    }

    @Override
    public void logError(String message, Throwable exception, String context) {
        // En prod, on log l'erreur mais avec moins de détails sur certaines exceptions
        if (isSensitiveException(exception)) {
            // Pour les exceptions sensibles, on masque certains détails
            log.error("[PROD][ERROR] {} - {}", context, sanitizeMessage(message));
        } else {
            log.error("[PROD][ERROR] {} - {}", context, message, exception);
        }
    }

    private boolean isBusinessCriticalContext(String context) {
        return context.startsWith("fr.lpreaux.usermanager.domain") ||
                context.contains("Service") ||
                context.contains("Controller");
    }

    private boolean isEssentialContext(String key) {
        // Liste des clés de contexte essentielles à conserver
        return "userId".equals(key) ||
                "requestId".equals(key) ||
                "traceId".equals(key) ||
                "spanId".equals(key);
    }

    private boolean isSensitiveException(Throwable exception) {
        // Identifier les exceptions qui pourraient contenir des informations sensibles
        return exception.getClass().getName().contains("Security") ||
                exception.getClass().getName().contains("Auth") ||
                exception.getMessage() != null &&
                        (exception.getMessage().contains("password") ||
                                exception.getMessage().contains("credential"));
    }

    private String sanitizeMessage(String message) {
        // Masquer les informations potentiellement sensibles
        if (message == null) {
            return "N/A";
        }
        return message.replaceAll("(?i)(password|secret|token|key)=\\S+", "$1=*****");
    }
}
