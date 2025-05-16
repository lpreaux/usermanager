package fr.lpreaux.usermanager.infrastructure.adapter.in.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.LoginRequest;
import fr.lpreaux.usermanager.infrastructure.security.BruteForceProtectionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtre pour la protection contre les attaques par force brute.
 * Vérifie les tentatives d'authentification et applique des restrictions
 * basées sur différentes métriques (IP, nom d'utilisateur, session).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)  // Juste après SecurityHeadersFilter
@Slf4j
@RequiredArgsConstructor
public class BruteForceProtectionFilter extends OncePerRequestFilter {

    private final BruteForceProtectionService bruteForceProtectionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Ne s'applique qu'aux requêtes d'authentification
        if (isAuthenticationRequest(request)) {
            // Avant de traiter la requête
            String username = extractUsername(request);

            // Vérifier si l'IP ou le nom d'utilisateur est bloqué
            if (bruteForceProtectionService.isBlocked(request, username)) {
                sendBlockedResponse(response);
                return;
            }

            // Wrapper la requête pour pouvoir la relire plus tard (si besoin)
            ContentCachingRequestWrapper wrappedRequest = wrapRequest(request);

            // Wrapper la réponse pour vérifier son statut
            StatusTrackingResponseWrapper wrappedResponse = new StatusTrackingResponseWrapper(response);

            Instant startTime = Instant.now();

            try {
                // Continuer la chaîne de filtres
                filterChain.doFilter(wrappedRequest, wrappedResponse);

                // Après traitement, vérifier si l'authentification a échoué
                if (isFailedAuthentication(wrappedResponse)) {
                    username = extractUsername(wrappedRequest);  // Réextraction avec la requête wrappée

                    // Enregistrer l'échec d'authentification
                    boolean blocked = bruteForceProtectionService.registerFailedAttempt(wrappedRequest, username);

                    // Si ce nouvel échec a déclenché un blocage, mettre à jour la réponse
                    if (blocked) {
                        // Réinitialiser la réponse
                        wrappedResponse.reset();
                        sendBlockedResponse(wrappedResponse);
                    }
                } else if (isSuccessfulAuthentication(wrappedResponse)) {
                    // Si l'authentification a réussi, réinitialiser les compteurs
                    username = extractUsername(wrappedRequest);
                    bruteForceProtectionService.resetAttempts(wrappedRequest, username);
                }
            } finally {
                // Logging des performances (utile pour détecter les tentatives de timing attack)
                Duration processingTime = Duration.between(startTime, Instant.now());
                if (processingTime.toMillis() > 500) {  // Seuil arbitraire
                    log.warn("Slow authentication processing detected: {} ms for request {}",
                            processingTime.toMillis(), wrappedRequest.getRequestURI());
                }
            }
        } else {
            // Pour les autres requêtes, passer simplement au filtre suivant
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Détermine si la requête est une tentative d'authentification
     */
    private boolean isAuthenticationRequest(HttpServletRequest request) {
        return request.getMethod().equals("POST") &&
                (request.getRequestURI().endsWith("/api/v1/auth/login") ||
                        request.getRequestURI().endsWith("/login"));
    }

    /**
     * Extrait le nom d'utilisateur de la requête
     */
    private String extractUsername(HttpServletRequest request) {
        // Cas où le nom d'utilisateur est dans les paramètres de requête
        String username = request.getParameter("username");
        if (username == null) {
            username = request.getParameter("login");
        }

        // Cas où la requête est en JSON
        if (username == null && request.getContentType() != null &&
                request.getContentType().contains(MediaType.APPLICATION_JSON_VALUE)) {
            try {
                // Pour les requêtes de type ContentCachingRequestWrapper,
                // nous pouvons obtenir le contenu
                if (request instanceof ContentCachingRequestWrapper wrapper) {
                    byte[] content = wrapper.getContentAsByteArray();
                    if (content.length > 0) {
                        LoginRequest loginRequest = objectMapper.readValue(content, LoginRequest.class);
                        username = loginRequest.login();
                    }
                }
            } catch (Exception e) {
                log.debug("Error extracting username from request body", e);
            }
        }

        return username;
    }

    /**
     * Vérifie si l'authentification a échoué
     */
    private boolean isFailedAuthentication(StatusTrackingResponseWrapper response) {
        // Status 401 (Unauthorized) ou 403 (Forbidden) indique un échec d'authentification
        return response.getStatus() == HttpStatus.UNAUTHORIZED.value() ||
                response.getStatus() == HttpStatus.FORBIDDEN.value();
    }

    /**
     * Vérifie si l'authentification a réussi
     */
    private boolean isSuccessfulAuthentication(StatusTrackingResponseWrapper response) {
        // Status 200 (OK) avec le bon chemin indique une authentification réussie
        return response.getStatus() == HttpStatus.OK.value();
    }

    /**
     * Envoie une réponse de blocage
     */
    private void sendBlockedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "Too many failed authentication attempts. Please try again later.");
        errorResponse.put("timestamp", System.currentTimeMillis());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Wrapper la requête pour pouvoir la relire
     */
    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }
        if (request instanceof MultipartHttpServletRequest) {
            return new ContentCachingRequestWrapper(request) {
                @Override
                public String getContentType() {
                    return request.getContentType();
                }
            };
        }
        return new ContentCachingRequestWrapper(request);
    }

    /**
     * Classe privée pour suivre le statut de la réponse
     */
    private static class StatusTrackingResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private int status = HttpStatus.OK.value();

        public StatusTrackingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int status) {
            this.status = status;
            super.setStatus(status);
        }

        @Override
        public int getStatus() {
            return status;
        }

        /**
         * Réinitialise la réponse
         */
        public void reset() {
            super.reset();
            status = HttpStatus.OK.value();
        }
    }
}