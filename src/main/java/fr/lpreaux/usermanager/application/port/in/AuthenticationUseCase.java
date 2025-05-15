package fr.lpreaux.usermanager.application.port.in;

import java.util.Set;

/**
 * Port d'entrée pour les opérations d'authentification.
 */
public interface AuthenticationUseCase {

    /**
     * Command pour l'authentification d'un utilisateur.
     */
    record AuthenticateCommand(
            String login,
            String password
    ) {}

    /**
     * DTO pour les informations de l'utilisateur authentifié.
     */
    record AuthenticationResultDTO(
            String userId,
            String login,
            String token,
            Set<String> roles,
            Set<String> permissions,
            long expiresAt
    ) {}

    /**
     * Authentifie un utilisateur et génère un token JWT.
     *
     * @param command Les informations d'authentification
     * @return Le résultat de l'authentification avec le token JWT
     * @throws fr.lpreaux.usermanager.application.exception.InvalidCredentialsException Si les identifiants sont invalides
     */
    AuthenticationResultDTO authenticate(AuthenticateCommand command);

    /**
     * Valide un token JWT et renvoie les informations utilisateur.
     *
     * @param token Le token JWT à valider
     * @return Les informations de l'utilisateur si le token est valide
     * @throws fr.lpreaux.usermanager.application.exception.InvalidTokenException Si le token est invalide ou expiré
     */
    AuthenticationResultDTO validateToken(String token);

    /**
     * Rafraîchit un token JWT existant.
     *
     * @param token Le token JWT à rafraîchir
     * @return Un nouveau token JWT
     * @throws fr.lpreaux.usermanager.application.exception.InvalidTokenException Si le token est invalide ou expiré
     */
    AuthenticationResultDTO refreshToken(String token);
}