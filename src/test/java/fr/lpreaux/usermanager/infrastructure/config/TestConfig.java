package fr.lpreaux.usermanager.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.posthog.java.PostHog;
import org.mockito.Mockito;

/**
 * Configuration de test qui remplace les services externes par des mocks
 * pour éviter les dépendances externes lors des tests.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Fournit un mock de PostHog pour les tests.
     * Cela évite d'avoir besoin d'une véritable clé API PostHog.
     */
    @Bean
    @Primary
    public PostHog postHogMock() {
        return Mockito.mock(PostHog.class);
    }
}