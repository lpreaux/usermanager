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

    @Override
    public void addToBlacklist(String token, long expirationTimeMs) {
        String key = TOKEN_PREFIX + token;
        long ttlMs = expirationTimeMs - System.currentTimeMillis();

        if (ttlMs > 0) {
            log.debug("Adding token to blacklist with TTL: {} ms", ttlMs);

            // Stocker des métadonnées supplémentaires peut être utile pour le débogage
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
        }

        return exists;
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Nettoyage toutes les heures (si nécessaire)
    public void removeExpiredTokens() {
        // Redis gère automatiquement l'expiration des clés
        // Cette méthode est principalement pour la conformité avec l'interface
        // et pourrait servir à d'autres actions de maintenance
        log.debug("Redis automatically manages token expiration, no manual cleanup needed");
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

        // En pratique, il faudrait avoir un mécanisme pour enregistrer tous les tokens
        // d'un utilisateur pour pouvoir les révoquer individuellement
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
}