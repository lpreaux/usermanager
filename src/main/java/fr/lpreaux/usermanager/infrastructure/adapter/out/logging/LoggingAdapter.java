package fr.lpreaux.usermanager.infrastructure.adapter.out.logging;

import java.util.Map;

import fr.lpreaux.usermanager.infrastructure.adapter.out.logging.strategy.LoggingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adaptateur de logging qui utilise la stratégie appropriée en fonction de l'environnement.
 * Fournit une API uniforme pour le logging dans toute l'application.
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingAdapter {

    private final LoggingStrategy loggingStrategy;

    /**
     * Log un message de niveau INFO
     *
     * @param message Message à logger
     * @param context Contexte du log (généralement la classe appelante)
     */
    public void info(String message, String context) {
        if (loggingStrategy.shouldLog("INFO", context)) {
            String formattedMessage = loggingStrategy.formatMessage("INFO", message, context);
            log.info(formattedMessage);
        }
    }

    /**
     * Log un message de niveau DEBUG
     *
     * @param message Message à logger
     * @param context Contexte du log (généralement la classe appelante)
     */
    public void debug(String message, String context) {
        if (loggingStrategy.shouldLog("DEBUG", context)) {
            String formattedMessage = loggingStrategy.formatMessage("DEBUG", message, context);
            log.debug(formattedMessage);
        }
    }

    /**
     * Log un message de niveau WARN
     *
     * @param message Message à logger
     * @param context Contexte du log (généralement la classe appelante)
     */
    public void warn(String message, String context) {
        if (loggingStrategy.shouldLog("WARN", context)) {
            String formattedMessage = loggingStrategy.formatMessage("WARN", message, context);
            log.warn(formattedMessage);
        }
    }

    /**
     * Log un message de niveau ERROR
     *
     * @param message Message à logger
     * @param exception Exception associée
     * @param context Contexte du log (généralement la classe appelante)
     */
    public void error(String message, Throwable exception, String context) {
        if (loggingStrategy.shouldLog("ERROR", context)) {
            loggingStrategy.logError(message, exception, context);
        }
    }

    /**
     * Log un message de niveau ERROR sans exception
     *
     * @param message Message à logger
     * @param context Contexte du log (généralement la classe appelante)
     */
    public void error(String message, String context) {
        if (loggingStrategy.shouldLog("ERROR", context)) {
            String formattedMessage = loggingStrategy.formatMessage("ERROR", message, context);
            log.error(formattedMessage);
        }
    }

    /**
     * Enrichit le contexte du log (MDC) avec des données supplémentaires
     *
     * @param context Données à ajouter au contexte
     */
    public void withContext(Map<String, String> context) {
        loggingStrategy.enrichContext(context);
    }

    /**
     * Nettoie le contexte après utilisation
     */
    public void clearContext() {
        loggingStrategy.clearContext();
    }

    /**
     * Utilitaire pour loguer une opération avec son contexte et sa durée
     *
     * @param operationName Nom de l'opération
     * @param context Contexte du log
     * @param runnable Opération à exécuter
     */
    public void logOperation(String operationName, String context, Runnable runnable) {
        long startTime = System.currentTimeMillis();
        info("Starting operation: " + operationName, context);

        try {
            runnable.run();
        } catch (Exception e) {
            error("Error during operation: " + operationName, e, context);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            info("Completed operation: " + operationName + " in " + duration + "ms", context);
        }
    }
}