package fr.lpreaux.usermanager.infrastructure.adapter.in.web.security;

import fr.lpreaux.usermanager.application.exception.InvalidTokenException;
import fr.lpreaux.usermanager.application.port.in.AuthenticationUseCase;
import fr.lpreaux.usermanager.application.port.out.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Filtre d'authentification JWT qui traite les tokens dans les headers HTTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationUseCase authenticationUseCase;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Extraire le token du header Authorization
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // Continuer la chaîne de filtres sans authentification
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        if (tokenBlacklistRepository.isBlacklisted(jwt)) {
            log.info("Request with blacklisted token rejected");
            // Ne PAS lever d'exception ici pour éviter de divulguer des informations
            // Passer simplement au filtre suivant sans authentifier
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Valider le token JWT
            AuthenticationUseCase.AuthenticationResultDTO authResult = authenticationUseCase.validateToken(jwt);

            // Créer une collection d'autorités à partir des rôles et permissions
            Collection<SimpleGrantedAuthority> authorities = Stream.concat(
                    authResult.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
                    authResult.permissions().stream().map(SimpleGrantedAuthority::new)
            ).collect(Collectors.toList());

            // Créer l'authentification Spring Security
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    authResult.userId(),
                    null, // Pas besoin de credentials car déjà authentifié via JWT
                    authorities
            );

            // Ajouter les détails de la requête
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Mettre à jour le contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Authenticated user: {}", authResult.login());

        } catch (InvalidTokenException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            // Ne pas jeter d'exception pour éviter d'exposer des informations sensibles
            // Continuer la chaîne sans authentification
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
        }

        // Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}