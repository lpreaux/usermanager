package fr.lpreaux.usermanager.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter userRegistrationCounter(MeterRegistry registry) {
        return Counter.builder("app.user.registration")
                .description("Number of user registrations")
                .register(registry);
    }

    @Bean
    public Counter failedLoginCounter(MeterRegistry registry) {
        return Counter.builder("app.user.login.failed")
                .description("Number of failed login attempts")
                .register(registry);
    }
}