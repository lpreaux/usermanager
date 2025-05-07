package fr.lpreaux.usermanager.infrastructure.adapter.in.web;

import fr.lpreaux.usermanager.application.port.in.*;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.request.*;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.dto.response.UserResponse;
import fr.lpreaux.usermanager.infrastructure.adapter.in.web.mapper.UserWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
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
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserQueryUseCase userQueryUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserWebMapper userWebMapper;

    /**
     * Register a new user.
     */
    @PostMapping
    public ResponseEntity<EntityModel<UserResponse>> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        log.info("Registering new user with login: {}", request.login());

        RegisterUserUseCase.RegisterUserCommand command = userWebMapper.toRegisterCommand(request);
        var userId = registerUserUseCase.registerUser(command);

        var userDetails = userQueryUseCase.findUserById(userId.getValue().toString())
                .orElseThrow(() -> new IllegalStateException("User not found after creation"));

        UserResponse response = userWebMapper.toUserResponse(userDetails);
        EntityModel<UserResponse> resource = EntityModel.of(response);
        resource.add(linkTo(methodOn(UserController.class).getUserById(userId.getValue().toString())).withSelfRel());

        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    /**
     * Get a user by ID.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<EntityModel<UserResponse>> getUserById(@PathVariable String userId) {
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
    public ResponseEntity<EntityModel<UserResponse>> updatePersonalInfo(
            @PathVariable String userId,
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
    public ResponseEntity<Void> changePassword(
            @PathVariable String userId,
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
    public ResponseEntity<EntityModel<UserResponse>> addEmail(
            @PathVariable String userId,
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
    public ResponseEntity<EntityModel<UserResponse>> removeEmail(
            @PathVariable String userId,
            @PathVariable String email) {
        log.info("Removing email {} from user ID: {}", email, userId);

        UpdateUserUseCase.RemoveEmailCommand command = new UpdateUserUseCase.RemoveEmailCommand(userId, email);
        updateUserUseCase.removeEmail(command);

        return getUserById(userId);
    }

    /**
     * Add a phone number to a user.
     */
    @PostMapping("/{userId}/phone-numbers")
    public ResponseEntity<EntityModel<UserResponse>> addPhoneNumber(
            @PathVariable String userId,
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
    public ResponseEntity<EntityModel<UserResponse>> removePhoneNumber(
            @PathVariable String userId,
            @PathVariable String phoneNumber) {
        log.info("Removing phone number {} from user ID: {}", phoneNumber, userId);

        UpdateUserUseCase.RemovePhoneNumberCommand command = new UpdateUserUseCase.RemovePhoneNumberCommand(userId, phoneNumber);
        updateUserUseCase.removePhoneNumber(command);

        return getUserById(userId);
    }

    /**
     * Delete a user.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Deleting user with ID: {}", userId);

        deleteUserUseCase.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search for a user by email.
     */
    @GetMapping("/search/by-email")
    public ResponseEntity<EntityModel<UserResponse>> getUserByEmail(@RequestParam String email) {
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
    public ResponseEntity<EntityModel<UserResponse>> getUserByLogin(@RequestParam String login) {
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
