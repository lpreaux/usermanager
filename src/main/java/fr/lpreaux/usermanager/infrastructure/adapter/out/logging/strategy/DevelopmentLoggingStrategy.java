package fr.lpreaux.usermanager.infrastructure.adapter.out.logging.strategy;

import java.util.Map;

import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

/**
 * Stratégie de logging pour l'environnement de développement.
 * Plus verbeuse et détaillée pour faciliter le débogage.
 */
@Slf4j
public class DevelopmentLoggingStrategy implements LoggingStrategy {

    @Override
    public boolean shouldLog(String level, String context) {
        // En dev, on log tout
        return true;
    }

    @Override
    public String formatMessage(String level, String message, String context) {
        // Format plus détaillé pour le dev
        return String.format("[DEV][%s] %s - %s", level, context, message);
    }

    @Override
    public void enrichContext(Map<String, String> additionalContext) {
        // Ajouter les données de contexte au MDC
        if (additionalContext != null) {
            additionalContext.forEach(MDC::put);
        }
        // Ajouter un marqueur d'environnement
        MDC.put("env", "dev");
    }

    @Override
    public void clearContext() {
        // Nettoyer le MDC
        MDC.clear();
    }

    @Override
    public void logError(String message, Throwable exception, String context) {
        // En dev, on affiche la stack trace complète
        log.error("[DEV][ERROR] {} - {}", context, message, exception);
    }
}