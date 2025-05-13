package fr.lpreaux.usermanager.infrastructure.adapter.out.analytics;

import com.posthog.java.PostHog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service pour interagir avec PostHog.
 * Fournit des méthodes pour le tracking des événements et la vérification des feature flags.
 */
@Service
public class AnalyticsService {

    private final PostHog postHog;

    @Value("${posthog.enabled:true}")
    private boolean enabled;

    @Value("${posthog.environment:development}")
    private String environment;

    public AnalyticsService(PostHog postHog) {
        this.postHog = postHog;
    }

    /**
     * Enregistre un événement utilisateur.
     *
     * @param userId    Identifiant de l'utilisateur
     * @param eventName Nom de l'événement
     * @param properties Propriétés de l'événement (optionnel)
     */
    public void trackEvent(String userId, String eventName, Map<String, Object> properties) {
        if (!enabled) return;

        try {
            Map<String, Object> propertiesWithEnv = properties != null ? properties : Map.of();
            propertiesWithEnv.put("environment", environment);

            postHog.capture(userId, eventName, propertiesWithEnv);
        } catch (Exception e) {
            // Log l'erreur mais ne la propage pas pour éviter d'affecter le flux principal
            System.err.println("Erreur lors du tracking de l'événement: " + e.getMessage());
        }
    }

    /**
     * Enregistre une identification utilisateur avec ses propriétés.
     *
     * @param userId    Identifiant de l'utilisateur
     * @param userProperties Propriétés de l'utilisateur
     */
    public void identifyUser(String userId, Map<String, Object> userProperties) {
        if (!enabled) return;

        try {
            postHog.identify(userId, userProperties);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'identification de l'utilisateur: " + e.getMessage());
        }
    }

    /**
     * Vérifie si un feature flag est actif pour un utilisateur donné.
     *
     * @param userId    Identifiant de l'utilisateur
     * @param flagKey   Clé du feature flag
     * @return true si le flag est actif, false sinon
     */
    public boolean isFeatureEnabled(String userId, String flagKey) {
        if (!enabled) return false;

        try {
            return postHog.isFeatureFlagEnabled(flagKey, userId);
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du feature flag: " + e.getMessage());
            return false;
        }
    }

    //**
    // * Récupère les valeurs d'un feature flag pour un utilisateur.
    // *
    // * @param userId    Identifiant de l'utilisateur
    // * @param flagKey   Clé du feature flag
    // * @param defaultValue Valeur par défaut si le flag n'est pas trouvé
    // * @return La valeur du feature flag
    /* public <T> T getFeatureFlag(String userId, String flagKey, T defaultValue) {
        if (!enabled) return defaultValue;

        try {
            return postHog.getFeatureFlag(flagKey, userId);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du feature flag: " + e.getMessage());
            return defaultValue;
        }
    }*/

    /**
     * Regroupe plusieurs événements utilisateur sous une même session.
     *
     * @param userId    Identifiant de l'utilisateur
     * @param sessionId Identifiant de la session (optionnel, généré si null)
     * @return L'identifiant de la session
     */
    public String startSession(String userId, String sessionId) {
        if (!enabled) return "";

        String actualSessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();

        try {
            postHog.capture(userId, "$session_start", Map.of(
                    "session_id", actualSessionId,
                    "environment", environment
            ));
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la session: " + e.getMessage());
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
                    "environment", environment
            ));
        } catch (Exception e) {
            System.err.println("Erreur lors de la fin de la session: " + e.getMessage());
        }
    }
}