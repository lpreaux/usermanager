package fr.lpreaux.usermanager.application.port.in;

/**
 * Port d'entrée pour les opérations de déconnexion et gestion des sessions.
 */
public interface LogoutUseCase {

    /**
     * Command pour la déconnexion d'un utilisateur.
     */
    record LogoutCommand(
            String token,
            String userId,
            String deviceInfo
    ) {}

    /**
     * Déconnecte un utilisateur en révoquant son token JWT.
     *
     * @param command La commande contenant les informations nécessaires
     */
    void logout(LogoutCommand command);

    /**
     * Déconnecte un utilisateur de toutes ses sessions actives.
     *
     * @param userId L'ID de l'utilisateur
     */
    void logoutFromAllSessions(String userId);

    /**
     * Déconnecte un utilisateur de tous les appareils sauf l'actuel.
     *
     * @param userId L'ID de l'utilisateur
     * @param currentToken Le token JWT actuel à conserver
     */
    void logoutFromOtherDevices(String userId, String currentToken);
}