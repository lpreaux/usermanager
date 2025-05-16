package fr.lpreaux.usermanager.infrastructure.adapter.out.logging.strategy;

/**
 * Interface pour les stratégies de logging.
 * Permet de définir comment les logs sont formatés et filtrés selon l'environnement.
 */
public interface LoggingStrategy {

    /**
     * Détermine si un log doit être enregistré en fonction de son niveau et de son contexte
     *
     * @param level Niveau de log (INFO, DEBUG, WARN, ERROR)
     * @param context Contexte du log (classe, méthode, etc.)
     * @return true si le log doit être enregistré, false sinon
     */
    boolean shouldLog(String level, String context);

    /**
     * Formate un message de log selon la stratégie de l'environnement
     *
     * @param level Niveau de log
     * @param message Message à logger
     * @param context Contexte du log
     * @return Message formaté
     */
    String formatMessage(String level, String message, String context);

    /**
     * Enrichit les données de contexte du log (MDC)
     *
     * @param additionalContext Données supplémentaires à ajouter au contexte
     */
    void enrichContext(java.util.Map<String, String> additionalContext);

    /**
     * Nettoie le contexte après l'exécution du log
     */
    void clearContext();

    /**
     * Gère les logs d'erreur avec des informations supplémentaires selon l'environnement
     *
     * @param message Message d'erreur
     * @param exception Exception associée
     * @param context Contexte du log
     */
    void logError(String message, Throwable exception, String context);
}