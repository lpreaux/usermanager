package fr.lpreaux.usermanager.infrastructure.config;

import com.posthog.java.PostHog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostHogConfig {

    @Value("${posthog.api-key}")
    private String apiKey;

    @Value("${posthog.host:https://app.posthog.com}")
    private String host;

    @Bean
    public PostHog postHog() {

        return new PostHog.Builder(apiKey)
                .host(host)
                .build();
    }
}