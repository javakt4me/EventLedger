package com.eventledger.eventgateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ResilienceConfig {

    @Bean
    @Primary
    // Provide a primary CircuitBreakerRegistry so tests and application code use the
    // test-friendly configuration for the accountService instance.
    public CircuitBreakerRegistry circuitBreakerRegistryCustom() {
        // Provide a conservative, test-friendly circuit breaker config for the accountService instance.
        CircuitBreakerConfig accountCbConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordExceptions(Throwable.class)
                .build();

        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        configs.put("accountService", accountCbConfig);

        return CircuitBreakerRegistry.of(configs);
    }
}
