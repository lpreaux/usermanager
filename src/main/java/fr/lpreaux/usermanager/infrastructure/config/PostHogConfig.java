package fr.lpreaux.usermanager.infrastructure.config;

import com.posthog.java.PostHog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class PostHogConfig {

    @Value("${posthog.api-key}")
    private String apiKey;

    @Value("${posthog.host:https://app.posthog.com}")
    private String host;

    private final Environment environment;

    public PostHogConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public PostHog postHog() {
        // Vous pouvez spécifier des propriétés supplémentaires ici

        return new PostHog.Builder(apiKey)
                .host(host)
                // Vous pouvez spécifier des propriétés supplémentaires ici
                .build();
    }
}