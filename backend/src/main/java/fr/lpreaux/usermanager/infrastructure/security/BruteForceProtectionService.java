package fr.lpreaux.usermanager.infrastructure.security;

import fr.lpreaux.usermanager.application.port.out.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service de protection contre les attaques par force brute.
 * Implémente un mécanisme de rate limiting basé sur différentes métriques
 * (IP, nom d'utilisateur, session) avec une politique d'échelle exponentielle.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BruteForceProtectionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityAuditLogger securityAuditLogger;

    // Préfixes pour les clés Redis
    private static final String AUTH_ATTEMPT_IP_PREFIX = "auth_attempt:ip:";
    private static final String AUTH_ATTEMPT_USERNAME_PREFIX = "auth_attempt:username:";
    private static final String AUTH_ATTEMPT_SESSION_PREFIX = "auth_attempt:session:";
    private static final String BLOCK_IP_PREFIX = "block:ip:";
    private static final String BLOCK_USERNAME_PREFIX = "block:username:";

    // Seuils et durées de blocage
    private static final int IP_MAX_ATTEMPTS = 10;
    private static final int USERNAME_MAX_ATTEMPTS = 5;
    private static final int SESSION_MAX_ATTEMPTS = 3;

    private static final Duration INITIAL_BLOCK_DURATION = Duration.ofMinutes(5);
    private static final Duration MAX_BLOCK_DURATION = Duration.ofHours(24);
    private static final int BLOCK_MULTIPLIER = 2;

    /**
     * Vérifie si une tentative d'authentification doit être bloquée.
     *
     * @param request  La requête HTTP
     * @param username Le nom d'utilisateur concerné
     * @return true si la tentative doit être bloquée
     */
    public boolean isBlocked(HttpServletRequest request, String username) {
        String ipAddress = getClientIp(request);
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "unknown";

        boolean ipBlocked = isIpBlocked(ipAddress);
        boolean usernameBlocked = isUsernameBlocked(username);

        if (ipBlocked || usernameBlocked) {
            // Journaliser l'événement de blocage
            securityAuditLogger.logSecurityEvent(
                    "authentication_blocked",
                    username,
                    ipAddress,
                    false,
                    Map.of(
                            "session_id", sessionId,
                            "ip_blocked", ipBlocked,
                            "username_blocked", usernameBlocked
                    )
            );

            return true;
        }

        return false;
    }

    /**
     * Enregistre une tentative d'authentification échouée et bloque
     * l'IP/username si nécessaire.
     *
     * @param request  La requête HTTP
     * @param username Le nom d'utilisateur concerné
     * @return true si cette tentative a déclenché un blocage
     */
    public boolean registerFailedAttempt(HttpServletRequest request, String username) {
        String ipAddress = getClientIp(request);
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "unknown";
        boolean newBlockCreated = false;

        // Incrémenter le compteur pour l'IP
        long ipAttempts = incrementCounter(AUTH_ATTEMPT_IP_PREFIX + ipAddress, 1, TimeUnit.HOURS);
        if (ipAttempts >= IP_MAX_ATTEMPTS && !isIpBlocked(ipAddress)) {
            blockIp(ipAddress, calculateBlockDuration(BLOCK_IP_PREFIX + ipAddress));
            newBlockCreated = true;
        }

        // Incrémenter le compteur pour le nom d'utilisateur
        if (username != null && !username.isEmpty()) {
            long usernameAttempts = incrementCounter(AUTH_ATTEMPT_USERNAME_PREFIX + username, 1, TimeUnit.HOURS);
            if (usernameAttempts >= USERNAME_MAX_ATTEMPTS && !isUsernameBlocked(username)) {
                blockUsername(username, calculateBlockDuration(BLOCK_USERNAME_PREFIX + username));
                newBlockCreated = true;
            }
        }

        // Incrémenter le compteur pour la session
        if (!sessionId.equals("unknown")) {
            long sessionAttempts = incrementCounter(AUTH_ATTEMPT_SESSION_PREFIX + sessionId, 30, TimeUnit.MINUTES);
            if (sessionAttempts >= SESSION_MAX_ATTEMPTS) {
                // Pour les sessions, on ne bloque pas, mais on peut invalider la session
                // ou prendre d'autres mesures
                log.warn("Session {} exceeded maximum authentication attempts", sessionId);
            }
        }

        // Journaliser l'événement
        Map<String, Object> details = new HashMap<>();
        details.put("ip_address", ipAddress);
        details.put("session_id", sessionId);
        details.put("ip_attempts", ipAttempts);
        if (username != null && !username.isEmpty()) {
            details.put("username_attempts", incrementCounter(AUTH_ATTEMPT_USERNAME_PREFIX + username, 0, TimeUnit.SECONDS));
        }
        details.put("blocked", newBlockCreated);

        securityAuditLogger.logSecurityEvent(
                "failed_authentication_attempt",
                username,
                ipAddress,
                false,
                details
        );

        return newBlockCreated;
    }

    /**
     * Réinitialise les compteurs de tentatives pour un utilisateur après
     * une authentification réussie.
     *
     * @param request  La requête HTTP
     * @param username Le nom d'utilisateur concerné
     */
    public void resetAttempts(HttpServletRequest request, String username) {
        String ipAddress = getClientIp(request);
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "unknown";

        // Réinitialiser les compteurs
        redisTemplate.delete(AUTH_ATTEMPT_SESSION_PREFIX + sessionId);

        // Pour l'IP et le nom d'utilisateur, on ne réinitialise pas complètement
        // mais on réduit le compteur pour éviter les abus
        long ipAttempts = incrementCounter(AUTH_ATTEMPT_IP_PREFIX + ipAddress, -2, TimeUnit.HOURS);
        if (ipAttempts < 0) {
            redisTemplate.delete(AUTH_ATTEMPT_IP_PREFIX + ipAddress);
        }

        if (username != null && !username.isEmpty()) {
            long usernameAttempts = incrementCounter(AUTH_ATTEMPT_USERNAME_PREFIX + username, -2, TimeUnit.HOURS);
            if (usernameAttempts < 0) {
                redisTemplate.delete(AUTH_ATTEMPT_USERNAME_PREFIX + username);
            }
        }
    }

    /**
     * Vérifie si une IP est bloquée.
     *
     * @param ipAddress L'adresse IP à vérifier
     * @return true si l'IP est bloquée
     */
    public boolean isIpBlocked(String ipAddress) {
        return redisTemplate.hasKey(BLOCK_IP_PREFIX + ipAddress);
    }

    /**
     * Vérifie si un nom d'utilisateur est bloqué.
     *
     * @param username Le nom d'utilisateur à vérifier
     * @return true si le nom d'utilisateur est bloqué
     */
    public boolean isUsernameBlocked(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return redisTemplate.hasKey(BLOCK_USERNAME_PREFIX + username);
    }

    /**
     * Bloque une adresse IP pour une durée spécifiée.
     *
     * @param ipAddress L'adresse IP à bloquer
     * @param duration  La durée du blocage
     */
    public void blockIp(String ipAddress, Duration duration) {
        redisTemplate.opsForValue().set(
                BLOCK_IP_PREFIX + ipAddress,
                String.valueOf(System.currentTimeMillis()),
                duration
        );

        log.warn("IP address {} has been blocked for {} minutes", ipAddress, duration.toMinutes());
    }

    /**
     * Bloque un nom d'utilisateur pour une durée spécifiée.
     *
     * @param username Le nom d'utilisateur à bloquer
     * @param duration La durée du blocage
     */
    public void blockUsername(String username, Duration duration) {
        if (username == null || username.isEmpty()) {
            return;
        }

        redisTemplate.opsForValue().set(
                BLOCK_USERNAME_PREFIX + username,
                String.valueOf(System.currentTimeMillis()),
                duration
        );

        log.warn("Username {} has been blocked for {} minutes", username, duration.toMinutes());
    }

    /**
     * Calcule la durée de blocage basée sur l'historique de blocages.
     * La durée augmente exponentiellement avec le nombre de blocages précédents.
     *
     * @param blockKey La clé Redis du blocage
     * @return La durée du blocage
     */
    private Duration calculateBlockDuration(String blockKey) {
        // Vérifier l'historique des blocages (stocker dans une autre clé)
        String blockHistoryKey = blockKey + ":history";
        Long blockCount = redisTemplate.opsForValue().increment(blockHistoryKey);

        if (blockCount == null || blockCount <= 1) {
            // Premier blocage
            redisTemplate.expire(blockHistoryKey, 30, TimeUnit.DAYS);
            return INITIAL_BLOCK_DURATION;
        } else {
            // Blocages répétés, augmenter exponentiellement
            long minutes = Math.min(
                    INITIAL_BLOCK_DURATION.toMinutes() * (long) Math.pow(BLOCK_MULTIPLIER, blockCount - 1),
                    MAX_BLOCK_DURATION.toMinutes()
            );
            return Duration.ofMinutes(minutes);
        }
    }

    /**
     * Incrémente un compteur et définit son expiration.
     *
     * @param key      La clé du compteur
     * @param amount   La valeur à ajouter (peut être négative)
     * @param timeUnit L'unité de temps pour l'expiration
     * @return La nouvelle valeur du compteur
     */
    private long incrementCounter(String key, long amount, TimeUnit timeUnit) {
        Long value = redisTemplate.opsForValue().increment(key, amount);

        // Si c'est une nouvelle clé, définir l'expiration
        if (value != null && value == amount && amount > 0) {
            switch (timeUnit) {
                case HOURS:
                    redisTemplate.expire(key, 1, TimeUnit.HOURS);
                    break;
                case MINUTES:
                    redisTemplate.expire(key, 30, TimeUnit.MINUTES);
                    break;
                default:
                    redisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
        }

        return value != null ? value : 0;
    }

    /**
     * Récupère l'adresse IP du client en tenant compte des proxys.
     *
     * @param request La requête HTTP
     * @return L'adresse IP du client
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}