package fr.lpreaux.usermanager.infrastructure.adapter.out.security;

import fr.lpreaux.usermanager.application.exception.InvalidTokenException;
import fr.lpreaux.usermanager.application.port.out.JwtTokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Implémentation de JwtTokenProvider utilisant la bibliothèque jjwt.
 */
@Component
@Primary
@Slf4j
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private final SecretKey secretKey;
    private final long tokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProviderImpl(
            @Value("${security.jwt.token.secret-key}") String secretKey,
            @Value("${security.jwt.token.expire-length-ms:3600000}") long tokenValidityMs,
            @Value("${security.jwt.token.refresh-expire-length-ms:604800000}") long refreshTokenValidityMs) {

        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityMs = tokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    @Override
    public String generateToken(String userId, String login, Set<String> roles,
                                Set<String> permissions, Map<String, Object> customClaims) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityMs);

        Claims claims = Jwts.claims()
                .subject(userId)
                .add("login", login)
                .add("roles", roles)
                .add("permissions", permissions)
                .issuedAt(now)
                .expiration(validity)
                .build();

        // Ajouter les custom claims
        if (customClaims != null) {
            claims.putAll(customClaims);
        }

        return Jwts.builder()
                .claims(claims)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public JwtTokenInfo validateToken(String token) {
        try {
            Jws<Claims> parsedToken = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = parsedToken.getPayload();

            String userId = claims.getSubject();
            String login = claims.get("login", String.class);

            @SuppressWarnings("unchecked")
            Set<String> roles = new HashSet<>(claims.get("roles", List.class));

            @SuppressWarnings("unchecked")
            Set<String> permissions = new HashSet<>(claims.get("permissions", List.class));

            long expiresAt = claims.getExpiration().getTime();

            return new JwtTokenInfo(userId, login, roles, permissions, expiresAt);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new InvalidTokenException("JWT token expired");
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT token");
        }
    }

    @Override
    public String refreshToken(String token) {
        try {
            JwtTokenInfo tokenInfo = validateToken(token);

            // Créer un nouveau token avec les mêmes informations mais une nouvelle date d'expiration
            return generateToken(
                    tokenInfo.userId(),
                    tokenInfo.login(),
                    tokenInfo.roles(),
                    tokenInfo.permissions(),
                    null
            );

        } catch (InvalidTokenException e) {
            log.warn("Cannot refresh invalid token: {}", e.getMessage());
            throw e;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid JWT token");
        }
    }
}