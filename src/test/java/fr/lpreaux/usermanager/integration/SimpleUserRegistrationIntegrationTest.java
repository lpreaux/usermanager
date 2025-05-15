package fr.lpreaux.usermanager.integration;

import fr.lpreaux.usermanager.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase.UserDetailsDTO;
import fr.lpreaux.usermanager.application.service.UserService;
import fr.lpreaux.usermanager.domain.model.valueobject.UserId;
import fr.lpreaux.usermanager.infrastructure.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration simples pour vérifier le flux complet d'enregistrement et récupération
 * d'un utilisateur. Chaque test est autonome.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public class SimpleUserRegistrationIntegrationTest {

    @Autowired
    private UserService userService;

    private RegisterUserCommand validCommand;
    private String userId;

    @BeforeEach
    void setUp() {
        // Initialiser une commande valide pour l'enregistrement
        validCommand = new RegisterUserCommand(
                "simple.test",
                "SecurePass123!",
                "Test",
                "Simple",
                LocalDate.of(1990, 1, 1),
                List.of("simple.test@example.com"),
                List.of("+33600000000")
        );
    }

    @AfterEach
    void tearDown() {
        // Si un utilisateur a été créé pendant le test, le supprimer
        if (userId != null) {
            try {
                userService.deleteUser(userId);
            } catch (Exception e) {
                // Ignorer les exceptions pendant le nettoyage
            }
        }
    }

    @Test
    @DisplayName("Should register and retrieve user successfully")
    void shouldRegisterAndRetrieveUser() {
        // When - Enregistrer un utilisateur
        UserId registeredUserId = userService.registerUser(validCommand);
        userId = registeredUserId.getValue().toString();

        // Then - Vérifier qu'il peut être récupéré
        Optional<UserDetailsDTO> retrievedUser = userService.findUserById(userId);

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().login()).isEqualTo("simple.test");
        assertThat(retrievedUser.get().emails()).containsExactly("simple.test@example.com");
        assertThat(retrievedUser.get().phoneNumbers()).containsExactly("+33600000000");
    }

    @Test
    @DisplayName("Should register and find user by email")
    void shouldFindUserByEmail() {
        // When - Enregistrer un utilisateur
        UserId registeredUserId = userService.registerUser(validCommand);
        userId = registeredUserId.getValue().toString();

        // Then - Vérifier qu'il peut être récupéré par email
        Optional<UserDetailsDTO> retrievedUser = userService.findUserByEmail("simple.test@example.com");

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().id()).isEqualTo(userId);
        assertThat(retrievedUser.get().login()).isEqualTo("simple.test");
    }

    @Test
    @DisplayName("Should register and find user by login")
    void shouldFindUserByLogin() {
        // When - Enregistrer un utilisateur
        UserId registeredUserId = userService.registerUser(validCommand);
        userId = registeredUserId.getValue().toString();

        // Then - Vérifier qu'il peut être récupéré par login
        Optional<UserDetailsDTO> retrievedUser = userService.findUserByLogin("simple.test");

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().id()).isEqualTo(userId);
        assertThat(retrievedUser.get().emails()).contains("simple.test@example.com");
    }
}