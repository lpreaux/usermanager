package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import fr.lpreaux.usermanager.application.port.in.RoleManagementUseCase;
import fr.lpreaux.usermanager.application.port.in.RoleManagementUseCase.RoleDTO;
import fr.lpreaux.usermanager.domain.model.valueobject.RoleId;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.CreateRoleRequest;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.UpdateRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Contrôleur REST pour la gestion des rôles.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Role Management", description = "API de gestion des rôles")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleManagementUseCase roleManagementUseCase;

    @PostMapping
    @Operation(
            summary = "Créer un rôle",
            description = "Crée un nouveau rôle avec les permissions spécifiées"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Rôle créé avec succès"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Requête invalide"
    )
    @ApiResponse(
            responseCode = "409",
            description = "Un rôle avec ce nom existe déjà"
    )
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<Void> createRole(@Valid @RequestBody CreateRoleRequest request) {
        log.info("Creating role: {}", request.name());

        RoleId roleId = roleManagementUseCase.createRole(
                new RoleManagementUseCase.CreateRoleCommand(
                        request.name(),
                        request.description(),
                        request.permissions()
                )
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(roleId.getValue())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping
    @Operation(
            summary = "Obtenir tous les rôles",
            description = "Récupère la liste de tous les rôles"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Liste des rôles récupérée avec succès",
            content = @Content(schema = @Schema(implementation = RoleDTO.class))
    )
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        log.debug("Getting all roles");

        List<RoleDTO> roles = roleManagementUseCase.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{roleId}")
    @Operation(
            summary = "Obtenir un rôle par ID",
            description = "Récupère les détails d'un rôle par son ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rôle trouvé",
            content = @Content(schema = @Schema(implementation = RoleDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Rôle non trouvé"
    )
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<RoleDTO> getRoleById(
            @Parameter(description = "ID du rôle") @PathVariable String roleId) {
        log.debug("Getting role with ID: {}", roleId);

        return roleManagementUseCase.findRoleById(roleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{roleId}")
    @Operation(
            summary = "Mettre à jour un rôle",
            description = "Met à jour les informations d'un rôle existant"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rôle mis à jour avec succès"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Rôle non trouvé"
    )
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<Void> updateRole(
            @Parameter(description = "ID du rôle") @PathVariable String roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.info("Updating role with ID: {}", roleId);

        roleManagementUseCase.updateRole(
                new RoleManagementUseCase.UpdateRoleCommand(
                        roleId,
                        request.name(),
                        request.description()
                )
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roleId}/permissions/{permission}")
    @Operation(
            summary = "Ajouter une permission à un rôle",
            description = "Ajoute une permission spécifique à un rôle"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Permission ajoutée avec succès"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Rôle non trouvé"
    )
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<Void> addPermission(
            @Parameter(description = "ID du rôle") @PathVariable String roleId,
            @Parameter(description = "Nom de la permission") @PathVariable String permission) {
        log.info("Adding permission {} to role {}", permission, roleId);

        roleManagementUseCase.addPermission(
                new RoleManagementUseCase.AddPermissionCommand(roleId, permission)
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleId}/permissions/{permission}")
    @Operation(
            summary = "Supprimer une permission d'un rôle",
            description = "Supprime une permission spécifique d'un rôle"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Permission supprimée avec succès"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Rôle non trouvé"
    )
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<Void> removePermission(
            @Parameter(description = "ID du rôle") @PathVariable String roleId,
            @Parameter(description = "Nom de la permission") @PathVariable String permission) {
        log.info("Removing permission {} from role {}", permission, roleId);

        roleManagementUseCase.removePermission(
                new RoleManagementUseCase.RemovePermissionCommand(roleId, permission)
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleId}")
    @Operation(
            summary = "Supprimer un rôle",
            description = "Supprime un rôle existant"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Rôle supprimé avec succès"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Rôle non trouvé"
    )
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "ID du rôle") @PathVariable String roleId) {
        log.info("Deleting role with ID: {}", roleId);

        roleManagementUseCase.deleteRole(roleId);

        return ResponseEntity.noContent().build();
    }
}