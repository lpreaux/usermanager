package fr.lpreaux.usermanager.infrastructure.config;

import fr.lpreaux.usermanager.application.port.in.RoleManagementUseCase;
import fr.lpreaux.usermanager.application.port.out.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Configuration pour initialiser les rôles et permissions par défaut.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RoleInitializationConfig {

    private final RoleRepository roleRepository;
    private final RoleManagementUseCase roleManagementUseCase;

    /**
     * Définition des rôles par défaut avec leurs permissions
     */
    private static final List<RoleDefinition> DEFAULT_ROLES = Arrays.asList(
            new RoleDefinition(
                    "ADMIN",
                    "Administrator role with full access",
                    Set.of("USER_CREATE", "USER_READ", "USER_UPDATE", "USER_DELETE",
                            "ROLE_CREATE", "ROLE_READ", "ROLE_UPDATE", "ROLE_DELETE")
            ),
            new RoleDefinition(
                    "USER",
                    "Regular user with basic access",
                    Set.of("USER_READ", "USER_UPDATE_SELF")
            ),
            new RoleDefinition(
                    "MODERATOR",
                    "Moderator with limited administrative privileges",
                    Set.of("USER_READ", "USER_UPDATE", "ROLE_READ")
            )
    );

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CommandLineRunner initializeRoles() {
        return args -> {
            log.info("Initializing default roles and permissions...");

            for (RoleDefinition definition : DEFAULT_ROLES) {
                if (!roleRepository.existsByName(definition.name())) {
                    try {
                        roleManagementUseCase.createRole(
                                new RoleManagementUseCase.CreateRoleCommand(
                                        definition.name(),
                                        definition.description(),
                                        definition.permissions()
                                )
                        );
                        log.info("Created role: {}", definition.name());
                    } catch (Exception e) {
                        log.error("Failed to create role {}: {}", definition.name(), e.getMessage());
                    }
                } else {
                    log.debug("Role already exists: {}", definition.name());
                }
            }

            log.info("Role initialization completed.");
        };
    }

    /**
     * Record pour définir un rôle avec ses permissions
     */
    private record RoleDefinition(String name, String description, Set<String> permissions) {}
}