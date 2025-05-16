package fr.lpreaux.usermanager.infrastructure.monitorig;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityMetrics {

    private final MeterRegistry meterRegistry;

    // Compteurs pour les événements d'authentification
    private Counter loginSuccessCounter;
    private Counter loginFailureCounter;
    private Counter tokenValidationCounter;
    private Counter tokenRefreshCounter;
    private Counter logoutCounter;
    private Counter blacklistedTokensCounter;
    private Counter rejectedTokensCounter;
    private Counter userBlacklistedCounter;

    @PostConstruct
    public void initCounters() {
        loginSuccessCounter = Counter.builder("auth.login.success")
                .description("Number of successful logins")
                .register(meterRegistry);

        loginFailureCounter = Counter.builder("auth.login.failure")
                .description("Number of failed logins")
                .register(meterRegistry);

        tokenValidationCounter = Counter.builder("auth.token.validation")
                .description("Number of token validations")
                .register(meterRegistry);

        tokenRefreshCounter = Counter.builder("auth.token.refresh")
                .description("Number of token refreshes")
                .register(meterRegistry);

        logoutCounter = Counter.builder("auth.logout")
                .description("Number of logouts")
                .register(meterRegistry);

        blacklistedTokensCounter = Counter.builder("auth.token.blacklisted")
                .description("Number of tokens added to blacklist")
                .register(meterRegistry);

        rejectedTokensCounter = Counter.builder("auth.token.rejected")
                .description("Number of rejected tokens due to blacklisting")
                .register(meterRegistry);

        userBlacklistedCounter = Counter.builder("auth.user.blacklisted")
                .description("Number of users with all tokens blacklisted")
                .register(meterRegistry);
    }

    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void incrementLoginFailure() {
        loginFailureCounter.increment();
    }

    public void incrementTokenValidation() {
        tokenValidationCounter.increment();
    }

    public void incrementTokenRefresh() {
        tokenRefreshCounter.increment();
    }

    public void incrementLogout() {
        logoutCounter.increment();
    }

    public void incrementBlacklistedTokens() {
        blacklistedTokensCounter.increment();
    }

    public void incrementRejectedTokens() {
        rejectedTokensCounter.increment();
    }

    public void incrementUserBlacklisted() {
        userBlacklistedCounter.increment();
    }
}