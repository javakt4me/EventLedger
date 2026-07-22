package com.eventledger.eventgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void testRestTemplateConfig() {
        RestTemplateConfig config = new RestTemplateConfig();
        RestTemplate restTemplate = config.restTemplate(new RestTemplateBuilder());
        assertNotNull(restTemplate);
    }

    @Test
    void testObjectMapperConfig() {
        RestTemplateConfig config = new RestTemplateConfig();
        ObjectMapper mapper = config.objectMapper();
        assertNotNull(mapper);
    }

    @Test
    void testCircuitBreakerRegistry() {
        Resilience4jConfig config = new Resilience4jConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        assertNotNull(registry);
    }

    @Test
    void testCircuitBreakerConfig() {
        Resilience4jConfig config = new Resilience4jConfig();
        CircuitBreakerConfig cbConfig = config.circuitBreakerConfig();
        assertNotNull(cbConfig);
    }

    @Test
    void testAccountServiceCircuitBreaker() {
        Resilience4jConfig config = new Resilience4jConfig();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        CircuitBreakerConfig cbConfig = config.circuitBreakerConfig();
        CircuitBreaker cb = config.accountServiceCircuitBreaker(registry, cbConfig);
        assertNotNull(cb);
        assertEquals("accountService", cb.getName());
    }

    @Test
    void testRetryRegistry() {
        Resilience4jConfig config = new Resilience4jConfig();
        RetryRegistry retryRegistry = config.retryRegistry();
        assertNotNull(retryRegistry);
    }

    @Test
    void testRetryConfig() {
        Resilience4jConfig config = new Resilience4jConfig();
        RetryConfig retryConfig = config.retryConfig();
        assertNotNull(retryConfig);
        assertEquals(3, retryConfig.getMaxAttempts());
    }

    @Test
    void testAccountServiceRetry() {
        Resilience4jConfig config = new Resilience4jConfig();
        RetryRegistry retryRegistry = config.retryRegistry();
        RetryConfig retryConfig = config.retryConfig();
        Retry retry = config.accountServiceRetry(retryRegistry, retryConfig);
        assertNotNull(retry);
        assertEquals("accountService", retry.getName());
    }
}

