package fr.lpreaux.usermanager.infrastructure.adapter.out.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import com.posthog.java.PostHog;

import java.util.Map;
import java.util.UUID;

/**
 * Version améliorée du service d'analytics avec support pour différents environnements.
 * Cette classe remplace l'implémentation originale avec une version tenant compte de l'environnement.
 */
@Service
public class AnalyticsService {

    private final PostHog postHog;
    private final Environment environment;

    @Value("${posthog.enabled:true}")
    private boolean enabled;

    @Value("${posthog.environment:development}")
    private String environmentName;

    @Value("${posthog.debug-mode:false}")
    private boolean debugMode;

    public AnalyticsService(PostHog postHog, Environment environment) {
        this.postHog = postHog;
        this.environment = environment;
    }

    /**
     * Enregistre un événement utilisateur avec des propriétés adaptées à l'environnement.
     *
     * @param userId     Identifiant de l'utilisateur
     * @param eventName  Nom de l'événement
     * @param properties Propriétés de l'événement
     */
    public void trackEvent(String userId, String eventName, Map<String, Object> properties) {
        if (!enabled) return;

        try {
            // Enrichir avec des données d'environnement
            Map<String, Object> enrichedProps = EnrichmentUtil.enrichProperties(properties, environmentName);

            // Ajouter des propriétés de debug en dev
            if (isDevelopment() && debugMode) {
                enrichedProps.put("debug_timestamp", System.currentTimeMillis());
                enrichedProps.put("debug_active_profiles", String.join(",", environment.getActiveProfiles()));
            }

            // Ne pas tracker certains événements en production
            if (isProduction() && isInternalEvent(eventName)) {
                return;
            }

            // Seulement en dev, on log l'événement
            if (isDevelopment() && debugMode) {
                logEventForDebugging(userId, eventName, enrichedProps);
            }

            postHog.capture(userId, eventName, enrichedProps);
        } catch (Exception e) {
            // Ne pas perturber l'application en cas d'erreur
            if (isDevelopment()) {
                // Plus verbeux en dev
                System.err.println("Erreur lors du tracking de l'événement: " + e.getMessage());
                e.printStackTrace();
            } else {
                // Silencieux en prod
                System.err.println("Erreur PostHog: " + e.getClass().getName());
            }
        }
    }

    /**
     * Enregistre une identification utilisateur avec ses propriétés.
     *
     * @param userId         Identifiant de l'utilisateur
     * @param userProperties Propriétés de l'utilisateur
     */
    public void identifyUser(String userId, Map<String, Object> userProperties) {
        if (!enabled) return;

        try {
            // Enrichir avec des données d'environnement
            Map<String, Object> enrichedProps = EnrichmentUtil.enrichProperties(userProperties, environmentName);

            // Suppression de données sensibles en production
            if (isProduction()) {
                EnrichmentUtil.sanitizeProperties(enrichedProps);
            }

            postHog.identify(userId, enrichedProps);
        } catch (Exception e) {
            if (isDevelopment()) {
                System.err.println("Erreur lors de l'identification de l'utilisateur: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Vérifie si un feature flag est actif pour un utilisateur donné.
     *
     * @param userId  Identifiant de l'utilisateur
     * @param flagKey Clé du feature flag
     * @return true si le flag est actif, false sinon
     */
    public boolean isFeatureEnabled(String userId, String flagKey) {
        if (!enabled) return false;

        try {
            // En dev, certains flags peuvent être forcés
            if (isDevelopment() && shouldForceFlag(flagKey)) {
                return getDefaultFlagValue(flagKey);
            }

            return postHog.isFeatureFlagEnabled(flagKey, userId);
        } catch (Exception e) {
            if (isDevelopment()) {
                System.err.println("Erreur lors de la vérification du feature flag: " + e.getMessage());
            }
            return getDefaultFlagValue(flagKey);
        }
    }

    /**
     * Démarre une session utilisateur.
     *
     * @param userId    Identifiant de l'utilisateur
     * @param sessionId Identifiant de session optionnel
     * @return Identifiant de la session
     */
    public String startSession(String userId, String sessionId) {
        if (!enabled) return "";

        String actualSessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();

        try {
            postHog.capture(userId, "$session_start", Map.of(
                    "session_id", actualSessionId,
                    "environment", environmentName));
        } catch (Exception e) {
            if (isDevelopment()) {
                System.err.println("Erreur lors du démarrage de la session: " + e.getMessage());
            }
        }

        return actualSessionId;
    }

    /**
     * Termine une session utilisateur.
     *
     * @param userId    Identifiant de l'utilisateur
     * @param sessionId Identifiant de la session
     */
    public void endSession(String userId, String sessionId) {
        if (!enabled) return;

        try {
            postHog.capture(userId, "$session_end", Map.of(
                    "session_id", sessionId,
                    "environment", environmentName));
        } catch (Exception e) {
            if (isDevelopment()) {
                System.err.println("Erreur lors de la fin de la session: " + e.getMessage());
            }
        }
    }

    // Méthodes utilitaires

    private boolean isDevelopment() {
        return environment.matchesProfiles("dev");
    }

    private boolean isProduction() {
        return environment.matchesProfiles("prod");
    }

    private boolean isInternalEvent(String eventName) {
        // Événements techniques qui ne doivent pas être trackés en production
        return eventName.startsWith("debug_") ||
                eventName.startsWith("test_") ||
                eventName.equals("page_view_test");
    }

    private boolean shouldForceFlag(String flagKey) {
        // Flags qui peuvent être forcés en dev pour faciliter le test
        return flagKey.startsWith("dev_") ||
                flagKey.startsWith("test_") ||
                flagKey.endsWith("_beta");
    }

    private boolean getDefaultFlagValue(String flagKey) {
        // Valeurs par défaut pour certains flags en cas d'erreur
        if (flagKey.contains("_beta") && isDevelopment()) {
            return true;  // Activer les fonctionnalités beta en dev/staging
        }
        if (flagKey.contains("_rollout")) {
            return false; // Désactiver les rollouts progressifs par défaut
        }
        return false;     // Fallback général
    }

    private void logEventForDebugging(String userId, String eventName, Map<String, Object> properties) {
        System.out.println("DEBUG PostHog Event: " + eventName);
        System.out.println("  User: " + userId);
        System.out.println("  Environment: " + environmentName);
        System.out.println("  Properties: " + properties);
    }

    /**
     * Classe utilitaire pour l'enrichissement des données
     */
    private static class EnrichmentUtil {

        public static Map<String, Object> enrichProperties(Map<String, Object> properties, String environment) {
            Map<String, Object> enriched = new java.util.HashMap<>(properties != null ? properties : Map.of());
            enriched.put("environment", environment);
            enriched.put("client_timestamp", System.currentTimeMillis());
            return enriched;
        }

        public static void sanitizeProperties(Map<String, Object> properties) {
            // Supprimer ou masquer les données potentiellement sensibles
            properties.remove("ip");
            properties.remove("email");
            properties.remove("phone");

            if (properties.containsKey("name")) {
                properties.put("name", "***");
            }

            // D'autres règles de sanitization peuvent être ajoutées ici
        }
    }
}