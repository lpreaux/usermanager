package fr.lpreaux.usermanager.infrastructure.adapter.out.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import fr.lpreaux.usermanager.application.exception.InvalidTokenException;
import fr.lpreaux.usermanager.application.port.out.JwtTokenProvider;
import fr.lpreaux.usermanager.infrastructure.monitorig.SecurityMetrics;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;

/**
 * Implémentation avancée de JwtTokenProvider utilisant JWE (JWT Encrypted).
 * Cette implémentation ajoute une couche de chiffrement aux tokens JWT pour plus de sécurité.
 */
@Component
@Slf4j
public class JweTokenProviderImpl implements JwtTokenProvider {

    private final SecretKey jwtKey;
    private final byte[] encryptionKey;
    private final long tokenValidityMs;
    private final long refreshTokenValidityMs;
    private final SecurityMetrics securityMetrics;

    public JweTokenProviderImpl(
            @Value("${security.jwt.token.secret-key}") String secretKey,
            @Value("${security.jwt.token.encryption-key:${security.jwt.token.secret-key}}") String encryptionKey,
            @Value("${security.jwt.token.expire-length-ms:3600000}") long tokenValidityMs,
            @Value("${security.jwt.token.refresh-expire-length-ms:604800000}") long refreshTokenValidityMs,
            SecurityMetrics securityMetrics) {

        this.jwtKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.encryptionKey = encryptionKey.getBytes(StandardCharsets.UTF_8);
        this.tokenValidityMs = tokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
        this.securityMetrics = securityMetrics;
    }

    @Override
    public String generateToken(String userId, String login, Set<String> roles,
                                Set<String> permissions, Map<String, Object> customClaims) {
        try {
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

            // Générer le JWT signé
            String jwt = Jwts.builder()
                    .claims(claims)
                    .signWith(jwtKey, Jwts.SIG.HS256)
                    .compact();

            // Chiffrer le JWT pour créer un JWE
            return encryptToken(jwt);
        } catch (Exception e) {
            log.error("Error while generating token", e);
            throw new SecurityException("Token generation error", e);
        }
    }

    @Override
    public JwtTokenInfo validateToken(String token) {
        try {
            // Déchiffrer le JWE pour obtenir le JWT
            String jwt = decryptToken(token);

            // Valider le JWT
            Jws<Claims> parsedToken = Jwts.parser()
                    .verifyWith(jwtKey)
                    .build()
                    .parseSignedClaims(jwt);

            Claims claims = parsedToken.getPayload();

            String userId = claims.getSubject();
            String login = claims.get("login", String.class);

            @SuppressWarnings("unchecked")
            Set<String> roles = new HashSet<>(claims.get("roles", List.class));

            @SuppressWarnings("unchecked")
            Set<String> permissions = new HashSet<>(claims.get("permissions", List.class));

            long expiresAt = claims.getExpiration().getTime();

            securityMetrics.incrementTokenValidation();
            return new JwtTokenInfo(userId, login, roles, permissions, expiresAt);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new InvalidTokenException("JWT token expired");
        } catch (Exception e) {
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

    /**
     * Chiffre un token JWT en utilisant JWE (JWT Encrypted)
     *
     * @param jwt Le token JWT à chiffrer
     * @return Le token JWE
     * @throws JOSEException En cas d'erreur de chiffrement
     */
    private String encryptToken(String jwt) throws JOSEException {
        // Préparer l'entête JWE
        JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                .contentType("JWT") // Indiquer que le contenu est un JWT
                .build();

        // Créer la charge utile du JWE
        Payload payload = new Payload(jwt);

        // Créer le JWE
        JWEObject jweObject = new JWEObject(header, payload);

        // Chiffrer avec la clé de chiffrement
        jweObject.encrypt(new DirectEncrypter(encryptionKey));

        // Sérialiser le JWE
        return jweObject.serialize();
    }

    /**
     * Déchiffre un token JWE pour récupérer le JWT original
     *
     * @param token Le token JWE à déchiffrer
     * @return Le token JWT d'origine
     * @throws JOSEException En cas d'erreur de déchiffrement
     * @throws ParseException En cas d'erreur de parsing du JWE
     */
    private String decryptToken(String token) throws JOSEException, ParseException {
        // Parser le JWE
        JWEObject jweObject = JWEObject.parse(token);

        // Déchiffrer avec la clé de chiffrement
        jweObject.decrypt(new DirectDecrypter(encryptionKey));

        // Récupérer le JWT d'origine
        return jweObject.getPayload().toString();
    }

    /**
     * Extrait une claim spécifique du token
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrait toutes les claims du token
     */
    private Claims extractAllClaims(String token) {
        try {
            // Déchiffrer le JWE
            String jwt = decryptToken(token);

            // Extraire les claims
            return Jwts.parser()
                    .verifyWith(jwtKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid JWT token");
        }
    }
}