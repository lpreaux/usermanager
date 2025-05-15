package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import fr.lpreaux.usermanager.application.port.in.RoleManagementUseCase.RoleDTO;
import fr.lpreaux.usermanager.application.port.in.UserRoleUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Contrôleur REST pour la gestion des rôles des utilisateurs.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Roles", description = "Gestion des rôles utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserRoleController {

    private final UserRoleUseCase userRoleUseCase;

    @GetMapping
    @Operation(
            summary = "Obtenir les rôles d'un utilisateur",
            description = "Récupère tous les rôles associés à un utilisateur"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rôles récupérés avec succès",
            content = @Content(schema = @Schema(implementation = RoleDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Utilisateur non trouvé"
    )
    @PreAuthorize("hasAuthority('USER_READ') or #userId == authentication.principal")
    public ResponseEntity<Set<RoleDTO>> getUserRoles(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId) {
        log.debug("Getting roles for user with ID: {}", userId);

        Set<RoleDTO> roles = userRoleUseCase.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/permissions")
    @Operation(
            summary = "Obtenir les permissions d'un utilisateur",
            description = "Récupère toutes les permissions associées à un utilisateur via ses rôles"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Permissions récupérées avec succès"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Utilisateur non trouvé"
    )
    @PreAuthorize("hasAuthority('USER_READ') or #userId == authentication.principal")
    public ResponseEntity<Set<String>> getUserPermissions(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId) {
        log.debug("Getting permissions for user with ID: {}", userId);

        Set<String> permissions = userRoleUseCase.getUserPermissions(userId);
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/{roleId}")
    @Operation(
            summary = "Assigner un rôle à un utilisateur",
            description = "Ajoute un rôle spécifique à un utilisateur"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rôle assigné avec succès"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Utilisateur ou rôle non trouvé"
    )
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Void> assignRole(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Parameter(description = "ID du rôle") @PathVariable String roleId) {
        log.info("Assigning role {} to user {}", roleId, userId);

        userRoleUseCase.assignRole(
                new UserRoleUseCase.AssignRoleCommand(userId, roleId)
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleId}")
    @Operation(
            summary = "Retirer un rôle d'un utilisateur",
            description = "Supprime un rôle spécifique d'un utilisateur"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rôle retiré avec succès"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Utilisateur ou rôle non trouvé"
    )
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Void> removeRole(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Parameter(description = "ID du rôle") @PathVariable String roleId) {
        log.info("Removing role {} from user {}", roleId, userId);

        userRoleUseCase.removeRole(
                new UserRoleUseCase.RemoveRoleCommand(userId, roleId)
        );

        return ResponseEntity.ok().build();
    }
}