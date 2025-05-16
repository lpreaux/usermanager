package fr.lpreaux.usermanager.infrastructure.config;

import fr.lpreaux.usermanager.infrastructure.adapter.out.analytics.AnalyticsService;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur pour suivre les performances et l'utilisation des endpoints API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsInterceptor implements HandlerInterceptor {

    private final AnalyticsService analyticsService;
    private static final String START_TIME_ATTR = "requestStartTime";
    private static final String SESSION_ID_ATTR = "analyticsSessionId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Stocker le temps de début pour calculer la durée plus tard
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        // Extraire l'ID utilisateur de la requête (selon votre mécanisme d'authentification)
        String userId = extractUserId(request);

        // Si on a un utilisateur identifié, on peut démarrer une session analytique
        if (userId != null && !userId.isEmpty()) {
            // Vérifier si une session existe déjà, sinon en créer une
            String sessionId = (String) request.getSession().getAttribute(SESSION_ID_ATTR);
            if (sessionId == null) {
                sessionId = analyticsService.startSession(userId, null);
                request.getSession().setAttribute(SESSION_ID_ATTR, sessionId);
            }
        }

        return true; // Continuer la chaîne de traitement
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) {
        // Ne rien faire ici
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        try {
            // Calculer la durée de la requête
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

            // Extraire l'ID utilisateur
            String userId = extractUserId(request);

            // Si pas d'utilisateur identifié, utiliser une valeur anonyme
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }

            // Préparer les propriétés de l'événement
            Map<String, Object> properties = new HashMap<>();
            properties.put("endpoint", request.getRequestURI());
            properties.put("method", request.getMethod());
            properties.put("status", response.getStatus());
            properties.put("duration_ms", duration);
            properties.put("referrer", request.getHeader("referer"));
            properties.put("user_agent", request.getHeader("User-Agent"));

            // Ajouter l'information d'erreur si présente
            if (ex != null) {
                properties.put("error", ex.getClass().getSimpleName());
                properties.put("error_message", ex.getMessage());
            }

            // Suivre l'événement d'API
            analyticsService.trackEvent(userId, "api_request", properties);

            // Log pour débogage
            log.debug("API Request: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

        } catch (Exception e) {
            // Ne jamais laisser une erreur d'analytics perturber l'application
            log.error("Error in analytics tracking", e);
        }
    }

    /**
     * Extrait l'ID utilisateur de la requête.
     * Cette méthode doit être adaptée en fonction de votre système d'authentification.
     */
    private String extractUserId(HttpServletRequest request) {
        // Option 1: Si vous utilisez Spring Security avec UserDetails
        /*
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) auth.getPrincipal()).getUsername();
        }
        */

        // Option 2: Si vous utilisez des JWT et stockez l'ID dans un attribut de requête
        // return (String) request.getAttribute("userId");

        // Option 3: Pour les tests ou système simple, extraire d'un header personnalisé
        String userId = request.getHeader("X-User-ID");

        // Option 4: Extraire d'un paramètre de requête (pour les méthodes non-sécurisées)
        if (userId == null) {
            userId = request.getParameter("userId");
        }

        return userId;
    }
}
