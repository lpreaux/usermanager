package fr.lpreaux.usermanager.infrastructure.config;

import io.sentry.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration pour Sentry adaptée aux différents environnements.
 */
@Configuration
public class SentryConfig {

    /**
     * Configuration Sentry pour le développement
     */
    @Configuration
    @Profile("dev")
    public static class DevSentryConfig {

        @Bean
        public RestTemplate devRestTemplate(RestTemplateBuilder builder) {
            return builder
                    .connectTimeout(Duration.ofSeconds(5))
                    .readTimeout(Duration.ofSeconds(5))
                    .additionalInterceptors(sentryTracingInterceptor())
                    .build();
        }

        private ClientHttpRequestInterceptor sentryTracingInterceptor() {
            // Intercepteur qui trace toutes les requêtes HTTP en mode développement
            return (request, body, execution) -> {
                // Créer un span pour une visibilité totale en dev
                IScopes scopes = Sentry.getCurrentScopes();
                io.sentry.ISpan span = scopes.getSpan() != null
                        ? scopes.getSpan().startChild("http.client", request.getMethod() + " " + request.getURI())
                        : scopes.startTransaction("http.client", request.getMethod() + " " + request.getURI());

                span.setData("http.url", request.getURI().toString());
                span.setData("http.method", request.getMethod());

                try {
                    var response = execution.execute(request, body);
                    span.setData("http.status_code", response.getStatusCode().value());
                    span.setStatus(SpanStatus.OK);
                    span.finish();
                    return response;
                } catch (Exception e) {
                    span.setThrowable(e);
                    span.setStatus(SpanStatus.INTERNAL_ERROR);
                    span.finish();
                    throw e;
                }
            };
        }
    }

    /**
     * Configuration Sentry pour la production
     */
    @Configuration
    @Profile("prod")
    public static class ProdSentryConfig {

        @Bean
        public RestTemplate prodRestTemplate(RestTemplateBuilder builder) {
            return builder
                    .connectTimeout(Duration.ofSeconds(10))
                    .readTimeout(Duration.ofSeconds(20))
                    .additionalInterceptors(prodSentryTracingInterceptor())
                    .build();
        }

        private ClientHttpRequestInterceptor prodSentryTracingInterceptor() {
            // Intercepteur qui trace uniquement les requêtes HTTP qui échouent en production
            return (request, body, execution) -> {
                // En prod, on ne crée pas systématiquement des spans pour ne pas surcharger
                try {
                    return execution.execute(request, body);
                } catch (Exception e) {
                    // Créer un span uniquement en cas d'erreur
                    IScopes scopes = Sentry.getCurrentScopes();
                    if (scopes.getSpan() != null) {
                        io.sentry.ISpan span = scopes.getSpan().startChild("http.client.error",
                                request.getMethod() + " " + request.getURI());
                        span.setData("http.url", request.getURI().toString());
                        span.setData("http.method", request.getMethod());
                        span.setThrowable(e);
                        span.setStatus(SpanStatus.INTERNAL_ERROR);
                        span.finish();
                    }
                    throw e;
                }
            };
        }
    }

    /**
     * Liste des packages à inclure dans le tracing Sentry
     * @return Liste des packages considérés comme faisant partie de l'application
     */
    @Bean
    public List<String> sentryInAppPackages() {
        List<String> packages = new ArrayList<>();
        packages.add("fr.lpreaux.usermanager");
        return packages;
    }
}