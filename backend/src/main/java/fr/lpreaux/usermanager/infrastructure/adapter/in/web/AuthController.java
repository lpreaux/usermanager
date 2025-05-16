package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import fr.lpreaux.usermanager.application.port.in.AuthenticationUseCase;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.LoginRequest;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.RefreshTokenRequest;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.response.AuthenticationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour l'authentification.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API d'authentification")
public class AuthController {

    private final AuthenticationUseCase authenticationUseCase;

    @PostMapping("/login")
    @Operation(
            summary = "Authentification utilisateur",
            description = "Authentifie un utilisateur avec son login et mot de passe et génère un token JWT"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Authentification réussie",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Identifiants invalides"
    )
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.login());

        AuthenticationUseCase.AuthenticationResultDTO result = authenticationUseCase.authenticate(
                new AuthenticationUseCase.AuthenticateCommand(
                        request.login(),
                        request.password()
                )
        );

        return ResponseEntity.ok(mapToResponse(result));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Rafraîchir le token",
            description = "Génère un nouveau token JWT à partir d'un token valide"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Token rafraîchi avec succès",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Token invalide ou expiré"
    )
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");

        AuthenticationUseCase.AuthenticationResultDTO result = authenticationUseCase.refreshToken(request.token());

        return ResponseEntity.ok(mapToResponse(result));
    }

    private AuthenticationResponse mapToResponse(AuthenticationUseCase.AuthenticationResultDTO dto) {
        return new AuthenticationResponse(
                dto.token(),
                dto.userId(),
                dto.login(),
                dto.roles(),
                dto.permissions(),
                dto.expiresAt()
        );
    }
}