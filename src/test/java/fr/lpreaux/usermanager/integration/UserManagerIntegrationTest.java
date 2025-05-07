package fr.lpreaux.usermanager.integration;

import fr.lpreaux.usermanager.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import fr.lpreaux.usermanager.application.port.in.UserQueryUseCase.UserDetailsDTO;
import fr.lpreaux.usermanager.application.service.UserService;
import fr.lpreaux.usermanager.domain.model.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration complet traversant toutes les couches de l'architecture hexagonale.
 * Domain -> Application -> Infrastructure
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserManagementIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Should register and retrieve user through all hexagonal layers")
    void shouldRegisterAndRetrieveUserThroughAllLayers() {
        // Given - Préparer la commande (Port d'entrée)
        RegisterUserCommand command = new RegisterUserCommand(
                "integration.test",
                "SecurePass123!",
                "Test",
                "Integration",
                LocalDate.of(1990, 1, 1),
                List.of("integration@test.com"),
                List.of("+33600000000")
        );

        // When - Exécuter le cas d'usage (Couche Application)
        UserId userId = userService.registerUser(command);

        // Then - Vérifier via un autre port d'entrée
        Optional<UserDetailsDTO> retrievedUser = userService.findUserById(userId.getValue().toString());

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().login()).isEqualTo("integration.test");
        assertThat(retrievedUser.get().emails()).containsExactly("integration@test.com");

        // Vérifier que toutes les couches ont été traversées
        Optional<UserDetailsDTO> byEmail = userService.findUserByEmail("integration@test.com");
        assertThat(byEmail).isPresent();
        assertThat(byEmail.get().id()).isEqualTo(userId.getValue().toString());
    }
}