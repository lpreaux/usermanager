package fr.lpreaux.usermanager.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

/**
 * Configuration pour Prometheus/Micrometer adaptée aux différents environnements.
 * Permet d'adapter la collecte de métriques selon l'environnement.
 */
@Configuration
public class MetricsConfig {

    /**
     * Configuration des métriques pour l'environnement de développement.
     * Collection plus détaillée pour aider au développement.
     */
    @Configuration
    @Profile("dev")
    public static class DevMetricsConfig {

        @Bean
        public MeterRegistryCustomizer<MeterRegistry> devMeterRegistryCustomizer() {
            return registry -> {
                // Ajouter des tags communs pour l'environnement de développement
                registry.config()
                        .commonTags("environment", "development")
                        .meterFilter(MeterFilter.acceptNameStartsWith("http"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("jvm"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("system"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("process"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("app"));
            };
        }
    }

    /**
     * Configuration des métriques pour l'environnement de production.
     * Focus sur les métriques critiques et business.
     */
    @Configuration
    @Profile("prod")
    public static class ProdMetricsConfig {

        @Bean
        public MeterRegistryCustomizer<MeterRegistry> prodMeterRegistryCustomizer() {
            return registry -> {
                registry.config()
                        .commonTags("environment", "production")
                        // Métriques critiques pour la production
                        .meterFilter(MeterFilter.acceptNameStartsWith("http"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("jvm.memory"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("jvm.gc"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("app"))
                        .meterFilter(MeterFilter.acceptNameStartsWith("system.cpu"))
                        // Filtrer les métriques non critiques
                        .meterFilter(MeterFilter.deny(id ->
                                id.getName().startsWith("jvm.threads") ||
                                        id.getName().startsWith("jvm.classes") ||
                                        id.getName().startsWith("system.disk") ||
                                        id.getName().contains("hikari")));
            };
        }
    }
}