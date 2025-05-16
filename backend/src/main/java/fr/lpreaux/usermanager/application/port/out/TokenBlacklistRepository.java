package fr.lpreaux.usermanager.application.port.out;

/**
 * Port de sortie pour la gestion de la liste noire des tokens JWT.
 */
public interface TokenBlacklistRepository {
    /**
     * Ajoute un token à la liste noire jusqu'à son expiration.
     *
     * @param token Le token JWT à blacklister
     * @param expirationTimeMs Timestamp d'expiration du token en millisecondes
     */
    void addToBlacklist(String token, long expirationTimeMs);

    /**
     * Vérifie si un token est dans la liste noire.
     *
     * @param token Le token JWT à vérifier
     * @return true si le token est blacklisté, sinon false
     */
    boolean isBlacklisted(String token);

    /**
     * Supprime manuellement les tokens expirés de la liste noire.
     * Typiquement utilisé par un job de nettoyage périodique.
     */
    void removeExpiredTokens();

    /**
     * Récupère le nombre de tokens dans la liste noire.
     * Utile pour le monitoring.
     *
     * @return Le nombre de tokens blacklistés
     */
    long getBlacklistSize();

    /**
     * Blackliste tous les tokens d'un utilisateur spécifique.
     * Utile pour les révocations de masse en cas de compromission.
     *
     * @param userId L'identifiant de l'utilisateur
     * @param reason La raison de la révocation (pour audit)
     */
    void blacklistAllUserTokens(String userId, String reason);
}