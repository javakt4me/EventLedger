package com.eventledger.eventgateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(Resilience4jConfig.class);

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Bean
    public CircuitBreaker accountServiceCircuitBreaker(CircuitBreakerRegistry registry, CircuitBreakerConfig config) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("accountService", config);
        registry.getEventPublisher()
                .onEntryAdded(event -> logger.info("Circuit Breaker added: {}", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> logger.info("Circuit Breaker removed: {}", event.getRemovedEntry().getName()));
        return circuitBreaker;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(500, 2))
                .build();
    }

    @Bean
    public Retry accountServiceRetry(RetryRegistry registry, RetryConfig config) {
        return registry.retry("accountService", config);
    }
}

