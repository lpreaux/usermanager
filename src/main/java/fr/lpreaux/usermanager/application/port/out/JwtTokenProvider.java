package fr.lpreaux.usermanager.application.port.out;

import java.util.Map;
import java.util.Set;

/**
 * Port de sortie pour la génération et la validation des tokens JWT.
 */
public interface JwtTokenProvider {

    /**
     * Information extraite d'un token JWT.
     */
    record JwtTokenInfo(
            String userId,
            String login,
            Set<String> roles,
            Set<String> permissions,
            long expiresAt
    ) {}

    /**
     * Génère un token JWT.
     *
     * @param userId L'ID de l'utilisateur
     * @param login Le login de l'utilisateur
     * @param roles Les rôles de l'utilisateur
     * @param permissions Les permissions de l'utilisateur
     * @param customClaims Claims supplémentaires à inclure dans le token
     * @return Le token JWT généré
     */
    String generateToken(String userId, String login, Set<String> roles,
                         Set<String> permissions, Map<String, Object> customClaims);

    /**
     * Valide un token JWT et extrait ses informations.
     *
     * @param token Le token JWT à valider
     * @return Les informations extraites du token
     * @throws fr.lpreaux.usermanager.application.exception.InvalidTokenException Si le token est invalide ou expiré
     */
    JwtTokenInfo validateToken(String token);

    /**
     * Rafraîchit un token JWT.
     *
     * @param token Le token JWT à rafraîchir
     * @return Un nouveau token JWT
     * @throws fr.lpreaux.usermanager.application.exception.InvalidTokenException Si le token est invalide ou expiré
     */
    String refreshToken(String token);
}