package fr.lpreaux.usermanager.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Spring MVC pour enregistrer notre intercepteur d'analytics.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AnalyticsInterceptor analyticsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(analyticsInterceptor)
                // Appliquer l'intercepteur sur tous les endpoints API
                .addPathPatterns("/api/**")
                // Ignorer certains endpoints fréquemment appelés comme les health checks
                .excludePathPatterns("/api/health", "/api/v1/health", "/actuator/**", "/api/v1/admin/feature-flags/**");
    }
}
