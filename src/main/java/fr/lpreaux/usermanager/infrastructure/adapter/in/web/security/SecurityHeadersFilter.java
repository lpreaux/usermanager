package fr.lpreaux.usermanager.infrastructure.adapter.in.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtre de sécurité pour la protection contre les attaques XSS et l'injection de contenu.
 * Ce filtre valide les entrées et ajoute des en-têtes de sécurité à toutes les réponses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class SecurityHeadersFilter extends OncePerRequestFilter {

    // Patterns pour la détection de tentatives d'attaque XSS
    private static final Pattern[] XSS_PATTERNS = {
            // Balises script
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            // src='...'
            Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // Scripts lonesome
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // eval(...)
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // expression(...)
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // javascript:...
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            // vbscript:...
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            // alert(...)
            Pattern.compile("alert\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // onload=...
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // onclick=...
            Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // onerror=...
            Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    // Chemins exclus de la vérification XSS (auth, Swagger, etc.)
    private static final Set<String> EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/health",
            "/management"
    ));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Vérifier les paramètres et les headers pour détecter les attaques XSS
        if (!isExcludedPath(request.getRequestURI()) &&
                (request.getMethod().equals("POST") || request.getMethod().equals("PUT") || request.getMethod().equals("PATCH"))) {

            // Vérification des paramètres
            Set<String> parameterNames = request.getParameterMap().keySet();
            for (String parameter : parameterNames) {
                String[] values = request.getParameterValues(parameter);
                if (values != null) {
                    for (String value : values) {
                        if (isXssAttack(value)) {
                            log.warn("Potential XSS attack detected in parameter: {}", parameter);
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input detected");
                            return;
                        }
                    }
                }
            }

            // Vérification des headers
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && isXssAttack(userAgent)) {
                log.warn("Potential XSS attack detected in User-Agent header");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input detected");
                return;
            }

            String referer = request.getHeader("Referer");
            if (referer != null && isXssAttack(referer)) {
                log.warn("Potential XSS attack detected in Referer header");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input detected");
                return;
            }
        }

        // 2. Ajouter les en-têtes de sécurité à toutes les réponses
        // Content Security Policy
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                        "img-src 'self' data:; " +
                        "font-src 'self' https://cdnjs.cloudflare.com; " +
                        "connect-src 'self'");

        // Protection XSS pour les navigateurs modernes
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Empêcher le MIME-sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Contrôle du cadre (anti-clickjacking)
        response.setHeader("X-Frame-Options", "DENY");

        // Politique référent
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Politique de fonctionnalités
        response.setHeader("Feature-Policy",
                "camera 'none'; " +
                        "microphone 'none'; " +
                        "geolocation 'none'; " +
                        "payment 'none'");

        // 3. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }

    /**
     * Vérifie si le chemin est exclu de la vérification XSS
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Vérifie si la chaîne contient des motifs d'attaque XSS
     */
    private boolean isXssAttack(String value) {
        if (value == null) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                return true;
            }
        }

        return false;
    }
}