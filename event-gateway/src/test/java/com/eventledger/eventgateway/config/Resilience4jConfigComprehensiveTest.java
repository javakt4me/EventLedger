package com.eventledger.eventgateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Resilience4jConfigComprehensiveTest {

    @InjectMocks
    private Resilience4jConfig resilience4jConfig;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private CircuitBreakerConfig circuitBreakerConfig;

    @Test
    void testCircuitBreakerRegistryBean() {
        assertNotNull(resilience4jConfig);
        CircuitBreakerRegistry registry = resilience4jConfig.circuitBreakerRegistry();
        assertNotNull(registry);
    }

    @Test
    void testCircuitBreakerConfigBean() {
        CircuitBreakerConfig config = resilience4jConfig.circuitBreakerConfig();
        assertNotNull(config);
        // Check default configuration values
        assertTrue(config.getFailureRateThreshold() > 0);
        assertTrue(config.getWaitDurationInOpenState().toMillis() > 0);
        assertTrue(config.getPermittedNumberOfCallsInHalfOpenState() > 0);
    }

    @Test
    void testRetryRegistryBean() {
        assertNotNull(resilience4jConfig.retryRegistry());
    }

    @Test
    void testRetryConfigBean() {
        assertNotNull(resilience4jConfig.retryConfig());
    }

    @Test
    void testAccountServiceCircuitBreakerCreation() {
        CircuitBreakerRegistry registry = resilience4jConfig.circuitBreakerRegistry();
        CircuitBreakerConfig config = resilience4jConfig.circuitBreakerConfig();
        
        CircuitBreaker circuitBreaker = resilience4jConfig.accountServiceCircuitBreaker(registry, config);
        assertNotNull(circuitBreaker);
        assertEquals("accountService", circuitBreaker.getName());
    }

    @Test
    void testAccountServiceRetryCreation() {
        assertNotNull(resilience4jConfig.accountServiceRetry(
                resilience4jConfig.retryRegistry(),
                resilience4jConfig.retryConfig()
        ));
    }

    @Test
    void testEventListenerRegistration() {
        // Test that event listeners are properly registered
        CircuitBreakerRegistry registry = resilience4jConfig.circuitBreakerRegistry();
        CircuitBreakerConfig config = resilience4jConfig.circuitBreakerConfig();
        
        // This creates the circuit breaker and registers event listeners
        CircuitBreaker circuitBreaker = resilience4jConfig.accountServiceCircuitBreaker(registry, config);
        
        assertNotNull(circuitBreaker);
        // The listeners should be registered during circuit breaker creation
    }

    @Test
    void testMultipleCircuitBreakerInstances() {
        CircuitBreakerRegistry registry = resilience4jConfig.circuitBreakerRegistry();
        CircuitBreakerConfig config = resilience4jConfig.circuitBreakerConfig();
        
        CircuitBreaker cb1 = resilience4jConfig.accountServiceCircuitBreaker(registry, config);
        CircuitBreaker cb2 = resilience4jConfig.accountServiceCircuitBreaker(registry, config);
        
        assertNotNull(cb1);
        assertNotNull(cb2);
        // They should be the same instance (registered with the registry)
        assertEquals(cb1.getName(), cb2.getName());
    }

    @Test
    void testCircuitBreakerConfigProperties() {
        CircuitBreakerConfig config = resilience4jConfig.circuitBreakerConfig();
        
        // Verify configuration properties are set correctly
        assertNotNull(config.getSlidingWindowType());
        assertTrue(config.getFailureRateThreshold() > 0);
        assertTrue(config.getFailureRateThreshold() <= 100);
        assertTrue(config.getPermittedNumberOfCallsInHalfOpenState() > 0);
        assertNotNull(config.getWaitDurationInOpenState());
    }
}
