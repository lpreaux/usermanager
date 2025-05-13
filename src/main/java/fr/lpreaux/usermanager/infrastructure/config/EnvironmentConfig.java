package fr.lpreaux.usermanager.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import fr.lpreaux.usermanager.infrastructure.adapter.out.logging.LoggingAdapter;
import fr.lpreaux.usermanager.infrastructure.adapter.out.logging.strategy.DevelopmentLoggingStrategy;
import fr.lpreaux.usermanager.infrastructure.adapter.out.logging.strategy.LoggingStrategy;
import fr.lpreaux.usermanager.infrastructure.adapter.out.logging.strategy.ProductionLoggingStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration pour la gestion des environnements.
 * Cette classe fournit des beans spécifiques à chaque environnement.
 */
@Configuration
@Slf4j
public class EnvironmentConfig {

    private final Environment environment;

    public EnvironmentConfig(Environment environment) {
        this.environment = environment;
        log.info("Active profiles: {}", String.join(", ", environment.getActiveProfiles()));
    }

    /**
     * Retourne la stratégie de logging adaptée à l'environnement actif
     */
    @Bean
    public LoggingStrategy loggingStrategy() {
        if (isProd()) {
            log.info("Using Production logging strategy");
            return new ProductionLoggingStrategy();
        } else {
            log.info("Using Development logging strategy");
            return new DevelopmentLoggingStrategy();
        }
    }

    /**
     * Adaptateur de logging qui utilise la stratégie appropriée
     */
    @Bean
    public LoggingAdapter loggingAdapter(LoggingStrategy loggingStrategy) {
        return new LoggingAdapter(loggingStrategy);
    }

    /**
     * Configuration spécifique au développement
     */
    @Configuration
    @Profile("dev")
    public static class DevConfig {
        public DevConfig() {
            log.info("Initializing development configuration");
        }

        // Beans spécifiques au développement
    }

    /**
     * Configuration spécifique à la production
     */
    @Configuration
    @Profile("prod")
    public static class ProdConfig {
        public ProdConfig() {
            log.info("Initializing production configuration");
        }

        // Beans spécifiques à la production
    }

    private boolean isProd() {
        return environment.matchesProfiles("prod");
    }

    private boolean isDev() {
        return environment.matchesProfiles("dev");
    }
}