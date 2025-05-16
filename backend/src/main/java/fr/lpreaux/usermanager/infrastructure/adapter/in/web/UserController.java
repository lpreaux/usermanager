package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import fr.lpreaux.usermanager.application.port.in.*;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.*;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.response.UserResponse;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.mapper.UserWebMapper;
import fr.lpreaux.usermanager.infrastructure.adapter.out.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for user management.
 * Implements the adapter layer in the hexagonal architecture pattern.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Opérations pour la gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserQueryUseCase userQueryUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserWebMapper userWebMapper;
    private final AnalyticsService analyticsService;

    /**
     * Register a new user.
     */
    @PostMapping
    @Operation(summary = "Créer un nouvel utilisateur", description = "Enregistre un nouveau utilisateur avec les informations fournies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides fournies"),
            @ApiResponse(responseCode = "409", description = "Login ou email déjà existants")
    })
    public ResponseEntity<EntityModel<UserResponse>> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        log.info("Registering new user with login: {}", request.login());

        RegisterUserUseCase.RegisterUserCommand command = userWebMapper.toRegisterCommand(request);
        var userId = registerUserUseCase.registerUser(command);

        var userDetails = userQueryUseCase.findUserById(userId.getValue().toString())
                .orElseThrow(() -> new IllegalStateException("User not found after creation"));

        UserResponse response = userWebMapper.toUserResponse(userDetails);
        EntityModel<UserResponse> resource = EntityModel.of(response);
        resource.add(linkTo(methodOn(UserController.class).getUserById(userId.getValue().toString())).withSelfRel());

        // Tracking de l'événement d'inscription
        Map<String, Object> properties = new HashMap<>();
        properties.put("login", request.login());
        properties.put("has_phone", request.phoneNumbers() != null && !request.phoneNumbers().isEmpty());
        properties.put("email_count", request.emails().size());
        analyticsService.trackEvent(userId.getValue().toString(), "user_registered", properties);

        // Identification de l'utilisateur pour PostHog
        Map<String, Object> userProperties = new HashMap<>();
        userProperties.put("login", request.login());
        userProperties.put("name", request.firstName() + " " + request.lastName());
        userProperties.put("email", request.emails().getFirst());
        analyticsService.identifyUser(userId.getValue().toString(), userProperties);

        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    /**
     * Get a user by ID.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Obtenir un utilisateur par ID", description = "Récupère les détails complets d'un utilisateur par son identifiant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PreAuthorize("hasAuthority('USER_READ') or #userId == authentication.principal")
    public ResponseEntity<EntityModel<UserResponse>> getUserById(
            @Parameter(description = "ID de l'utilisateur à récupérer") @PathVariable String userId) {
        log.info("Getting user with ID: {}", userId);

        return userQueryUseCase.findUserById(userId)
                .map(userWebMapper::toUserResponse)
                .map(response -> {
                    EntityModel<UserResponse> resource = EntityModel.of(response);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(userId)).withSelfRel());
                    resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));
                    return ResponseEntity.ok(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all users.
     */
    @GetMapping
    @Operation(summary = "Obtenir tous les utilisateurs", description = "Récupère la liste complète des utilisateurs")
    @ApiResponse(responseCode = "200", description = "Liste des utilisateurs récupérée avec succès")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<CollectionModel<EntityModel<UserResponse>>> getAllUsers() {
        log.info("Getting all users");

        List<EntityModel<UserResponse>> users = userQueryUseCase.getAllUsers().stream()
                .map(userWebMapper::toUserResponse)
                .map(response -> {
                    EntityModel<UserResponse> resource = EntityModel.of(response);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(response.id())).withSelfRel());
                    return resource;
                })
                .collect(Collectors.toList());

        CollectionModel<EntityModel<UserResponse>> resources = CollectionModel.of(users);
        resources.add(linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel());

        return ResponseEntity.ok(resources);
    }

    /**
     * Update a user's personal information.
     */
    @PutMapping("/{userId}/personal-info")
    @Operation(summary = "Mettre à jour les informations personnelles", description = "Met à jour le nom, prénom et la date de naissance d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informations mises à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides fournies"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PreAuthorize("hasAuthority('USER_UPDATE') or #userId == authentication.principal")
    public ResponseEntity<EntityModel<UserResponse>> updatePersonalInfo(
            @Parameter(description = "ID de l'utilisateur à mettre à jour") @PathVariable String userId,
            @Valid @RequestBody UpdatePersonalInfoRequest request) {
        log.info("Updating personal info for user ID: {}", userId);

        UpdateUserUseCase.UpdatePersonalInfoCommand command = userWebMapper.toUpdatePersonalInfoCommand(userId, request);
        updateUserUseCase.updatePersonalInfo(command);

        return userQueryUseCase.findUserById(userId)
                .map(userWebMapper::toUserResponse)
                .map(response -> {
                    EntityModel<UserResponse> resource = EntityModel.of(response);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(userId)).withSelfRel());
                    return ResponseEntity.ok(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Change a user's password.
     */
    @PostMapping("/{userId}/password/change")
    @Operation(summary = "Changer le mot de passe", description = "Modifie le mot de passe d'un utilisateur après vérification du mot de passe actuel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Mot de passe changé avec succès"),
            @ApiResponse(responseCode = "400", description = "Mot de passe actuel incorrect ou nouveau mot de passe invalide"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PreAuthorize("#userId == authentication.principal")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Changing password for user ID: {}", userId);

        UpdateUserUseCase.ChangePasswordCommand command = userWebMapper.toChangePasswordCommand(userId, request);
        updateUserUseCase.changePassword(command);

        return ResponseEntity.noContent().build();
    }

    /**
     * Add an email to a user.
     */
    @PostMapping("/{userId}/emails")
    @Operation(summary = "Ajouter un email", description = "Ajoute une adresse email à un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email ajouté avec succès"),
            @ApiResponse(responseCode = "400", description = "Format d'email invalide"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé"),
            @ApiResponse(responseCode = "409", description = "Email déjà utilisé par un autre utilisateur")
    })
    @PreAuthorize("hasAuthority('USER_UPDATE') or #userId == authentication.principal")
    public ResponseEntity<EntityModel<UserResponse>> addEmail(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Valid @RequestBody AddEmailRequest request) {
        log.info("Adding email {} to user ID: {}", request.email(), userId);

        UpdateUserUseCase.AddEmailCommand command = userWebMapper.toAddEmailCommand(userId, request);
        updateUserUseCase.addEmail(command);

        return getUserById(userId);
    }

    /**
     * Remove an email from a user.
     */
    @DeleteMapping("/{userId}/emails/{email}")
    @Operation(summary = "Supprimer un email", description = "Supprime une adresse email d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email supprimé avec succès"),
            @ApiResponse(responseCode = "400", description = "Impossible de supprimer le dernier email"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PreAuthorize("hasAuthority('USER_UPDATE') or #userId == authentication.principal")
    public ResponseEntity<EntityModel<UserResponse>> removeEmail(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Parameter(description = "Email à supprimer") @PathVariable String email) {
        log.info("Removing email {} from user ID: {}", email, userId);

        UpdateUserUseCase.RemoveEmailCommand command = new UpdateUserUseCase.RemoveEmailCommand(userId, email);
        updateUserUseCase.removeEmail(command);

        return getUserById(userId);
    }

    /**
     * Add a phone number to a user.
     */
    @PostMapping("/{userId}/phone-numbers")
    @Operation(summary = "Ajouter un numéro de téléphone", description = "Ajoute un numéro de téléphone à un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Numéro de téléphone ajouté avec succès"),
            @ApiResponse(responseCode = "400", description = "Format de numéro de téléphone invalide"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PreAuthorize("hasAuthority('USER_UPDATE') or #userId == authentication.principal")
    public ResponseEntity<EntityModel<UserResponse>> addPhoneNumber(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Valid @RequestBody AddPhoneNumberRequest request) {
        log.info("Adding phone number {} to user ID: {}", request.phoneNumber(), userId);

        UpdateUserUseCase.AddPhoneNumberCommand command = userWebMapper.toAddPhoneNumberCommand(userId, request);
        updateUserUseCase.addPhoneNumber(command);

        return getUserById(userId);
    }

    /**
     * Remove a phone number from a user.
     */
    @DeleteMapping("/{userId}/phone-numbers/{phoneNumber}")
    @Operation(summary = "Supprimer un numéro de téléphone", description = "Supprime un numéro de téléphone d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Numéro de téléphone supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PreAuthorize("hasAuthority('USER_UPDATE') or #userId == authentication.principal")
    public ResponseEntity<EntityModel<UserResponse>> removePhoneNumber(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Parameter(description = "Numéro de téléphone à supprimer") @PathVariable String phoneNumber) {
        log.info("Removing phone number {} from user ID: {}", phoneNumber, userId);

        UpdateUserUseCase.RemovePhoneNumberCommand command = new UpdateUserUseCase.RemovePhoneNumberCommand(userId, phoneNumber);
        updateUserUseCase.removePhoneNumber(command);

        return getUserById(userId);
    }

    /**
     * Delete a user.
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Supprimer un utilisateur", description = "Supprime définitivement un utilisateur du système")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Utilisateur supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé")
    })
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID de l'utilisateur à supprimer") @PathVariable String userId) {
        log.info("Deleting user with ID: {}", userId);

        // Récupérer les données utilisateur avant suppression pour le tracking
        var userDetails = userQueryUseCase.findUserById(userId).orElse(null);

        // Effectuer la suppression
        deleteUserUseCase.deleteUser(userId);

        // Tracking de l'événement de suppression si l'utilisateur existait
        if (userDetails != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("login", userDetails.login());
            properties.put("account_age_days", userDetails.age());
            analyticsService.trackEvent(userId, "user_deleted", properties);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Search for a user by email.
     */
    @GetMapping("/search/by-email")
    @Operation(summary = "Rechercher un utilisateur par email", description = "Trouve un utilisateur par son adresse email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
            @ApiResponse(responseCode = "404", description = "Aucun utilisateur trouvé avec cet email")
    })
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<EntityModel<UserResponse>> getUserByEmail(
            @Parameter(description = "Email à rechercher") @RequestParam String email) {
        log.info("Searching user by email: {}", email);

        return userQueryUseCase.findUserByEmail(email)
                .map(userWebMapper::toUserResponse)
                .map(response -> {
                    EntityModel<UserResponse> resource = EntityModel.of(response);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(response.id())).withSelfRel());
                    resource.add(linkTo(methodOn(UserController.class).getUserByEmail(email)).withRel("search-by-email"));
                    return ResponseEntity.ok(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search for a user by login.
     */
    @GetMapping("/search/by-login")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Rechercher un utilisateur par login", description = "Trouve un utilisateur par son login")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé"),
            @ApiResponse(responseCode = "404", description = "Aucun utilisateur trouvé avec ce login")
    })
    public ResponseEntity<EntityModel<UserResponse>> getUserByLogin(
            @Parameter(description = "Login à rechercher") @RequestParam String login) {
        log.info("Searching user by login: {}", login);

        return userQueryUseCase.findUserByLogin(login)
                .map(userWebMapper::toUserResponse)
                .map(response -> {
                    EntityModel<UserResponse> resource = EntityModel.of(response);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(response.id())).withSelfRel());
                    resource.add(linkTo(methodOn(UserController.class).getUserByLogin(login)).withRel("search-by-login"));
                    return ResponseEntity.ok(resource);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
