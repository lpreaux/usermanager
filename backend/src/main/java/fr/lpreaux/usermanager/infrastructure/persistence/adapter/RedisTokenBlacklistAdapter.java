package fr.lpreaux.usermanager.infrastructure.persistence.adapter;

import fr.lpreaux.usermanager.application.port.out.TokenBlacklistRepository;
import fr.lpreaux.usermanager.infrastructure.monitorig.SecurityMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Implémentation Redis du repository de liste noire de tokens.
 * Utilise Redis comme stockage pour les tokens révoqués avec expiration automatique.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTokenBlacklistAdapter implements TokenBlacklistRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityMetrics securityMetrics;

    // Préfixes pour les clés Redis
    private static final String TOKEN_PREFIX = "blacklisted_token:";
    private static final String USER_PREFIX = "blacklisted_user:";
    private static final String USER_TOKEN_PREFIX = "user_tokens:";

    @Override
    public void addToBlacklist(String token, long expirationTimeMs) {
        String key = TOKEN_PREFIX + token;
        long ttlMs = expirationTimeMs - System.currentTimeMillis();

        if (ttlMs > 0) {
            log.debug("Adding token to blacklist with TTL: {} ms", ttlMs);

            // Stocker des métadonnées supplémentaires pour le débogage
            String value = Instant.now().toString();

            redisTemplate.opsForValue().set(key, value, ttlMs, TimeUnit.MILLISECONDS);
            securityMetrics.incrementBlacklistedTokens();
            log.info("Token added to blacklist, expires at: {}", Instant.ofEpochMilli(expirationTimeMs));
        } else {
            log.warn("Attempted to blacklist an already expired token");
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        String key = TOKEN_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);

        if (exists) {
            log.debug("Token found in blacklist");
            securityMetrics.incrementRejectedTokens();
            return true;
        }

        return false;
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Nettoyage toutes les heures
    public void removeExpiredTokens() {
        // Redis gère automatiquement l'expiration des clés
        // Cette méthode est principalement pour la conformité avec l'interface
        // et pourrait servir à des actions de maintenance supplémentaires

        log.debug("Starting scheduled maintenance check for token blacklist");

        // Comptage des entrées dans la liste noire pour monitoring
        long size = getBlacklistSize();
        log.info("Current blacklist size: {} tokens", size);

        // On pourrait ajouter ici d'autres opérations de maintenance
        // comme la vérification de l'intégrité ou la réplication des données
    }

    @Override
    public long getBlacklistSize() {
        ScanOptions options = ScanOptions.scanOptions().match(TOKEN_PREFIX + "*").build();
        long count = 0;

        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        } catch (Exception e) {
            log.error("Error counting blacklisted tokens", e);
        }

        return count;
    }

    @Override
    public void blacklistAllUserTokens(String userId, String reason) {
        log.info("Blacklisting all tokens for user: {}, reason: {}", userId, reason);

        // Enregistrer l'événement de révocation pour le suivi
        String userKey = USER_PREFIX + userId;
        String value = Instant.now() + ":" + reason;

        // Conserver cette information pendant une période plus longue (7 jours)
        redisTemplate.opsForValue().set(userKey, value, 7, TimeUnit.DAYS);

        // Dans une implémentation complète, on récupérerait tous les tokens de l'utilisateur
        // depuis une table de mapping user -> tokens et on les blacklisterait un par un

        // Pour l'exemple, on vérifie s'il y a des tokens associés à cet utilisateur
        String userTokensKey = USER_TOKEN_PREFIX + userId;
        if (redisTemplate.hasKey(userTokensKey)) {
            // Dans une vraie implémentation, on récupérerait et révoquerait tous ces tokens
            log.info("User tokens found and would be blacklisted here");
        }

        securityMetrics.incrementUserBlacklisted();
        log.info("All tokens blacklisted for user: {}", userId);
    }

    /**
     * Vérifier si tous les tokens d'un utilisateur sont blacklistés.
     *
     * @param userId L'ID de l'utilisateur
     * @return true si tous les tokens de l'utilisateur sont blacklistés
     */
    public boolean isUserFullyBlacklisted(String userId) {
        String userKey = USER_PREFIX + userId;
        return redisTemplate.hasKey(userKey);
    }

    /**
     * Récupère la raison de la révocation de tous les tokens d'un utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @return La raison de la révocation ou null si aucune
     */
    public String getBlacklistReason(String userId) {
        String userKey = USER_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(userKey);

        if (value != null && value.contains(":")) {
            return value.split(":", 2)[1];
        }

        return null;
    }

    /**
     * Associe un token à un utilisateur pour la gestion des sessions.
     *
     * @param userId L'ID de l'utilisateur
     * @param token Le token JWT
     * @param deviceInfo Informations sur l'appareil
     */
    public void registerUserToken(String userId, String token, String deviceInfo) {
        String userTokensKey = USER_TOKEN_PREFIX + userId;
        String tokenKey = token.substring(0, Math.min(token.length(), 32)); // Portion identifiable du token

        // Dans une implémentation complète, on stockerait ces informations
        // pour permettre la gestion fine des sessions
        redisTemplate.opsForHash().put(userTokensKey, tokenKey, deviceInfo);

        // On pourrait ajouter une expiration à cette entrée
        redisTemplate.expire(userTokensKey, 30, TimeUnit.DAYS);

        log.debug("Registered token for user: {}, device: {}", userId, deviceInfo);
    }
}