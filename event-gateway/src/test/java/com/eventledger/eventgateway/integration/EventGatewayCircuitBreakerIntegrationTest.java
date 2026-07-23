package com.eventledger.eventgateway.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EventGatewayCircuitBreakerIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startWiremock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWiremock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("account.service.url", () -> "http://localhost:" + wireMockServer.port());
        // Make circuit breaker open quickly for test
        registry.add("resilience4j.circuitbreaker.instances.accountService.slidingWindowSize", () -> "2");
        registry.add("resilience4j.circuitbreaker.instances.accountService.failureRateThreshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.accountService.waitDurationInOpenState", () -> "2s");
        registry.add("resilience4j.circuitbreaker.instances.accountService.permittedNumberOfCallsInHalfOpenState", () -> "1");
        // Disable retry so call counts are predictable
        registry.add("resilience4j.retry.instances.accountService.maxAttempts", () -> "1");
    }

    @Test
    void testCircuitBreakerOpensAndRecovers() throws Exception {
        // Reset stubs
        wireMockServer.resetAll();

        // First two calls fail (500) to trigger circuit open
        wireMockServer.stubFor(post(urlPathMatching("/accounts/.*/transactions"))
                .willReturn(aResponse().withStatus(500)));

        String eventJsonTemplate = "{\"eventId\":\"evt-cb-%d\",\"accountId\":\"acct-cb\",\"type\":\"CREDIT\",\"amount\":10.00,\"currency\":\"USD\",\"eventTimestamp\":\"2020-01-01T00:00:00Z\",\"metadata\":{}}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        // Send two failing requests
        for (int i = 1; i <= 2; i++) {
            String json = String.format(eventJsonTemplate, i);
            ResponseEntity<Map> resp = restTemplate.postForEntity("http://localhost:" + port + "/events", new HttpEntity<>(json, headers), Map.class);
            // When downstream fails, gateway should return 503
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }

        // Circuit should be OPEN
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("accountService");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Now configure WireMock to return success
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlPathMatching("/accounts/.*/transactions"))
                .willReturn(aResponse().withStatus(200)));

        // Wait for the circuit's waitDurationInOpenState to pass and for the half-open trial
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        boolean closed = false;
        while (Instant.now().isBefore(deadline)) {
            // Trigger a request which will be allowed in half-open
            String json = String.format(eventJsonTemplate, 99);
            ResponseEntity<Map> resp = restTemplate.postForEntity("http://localhost:" + port + "/events", new HttpEntity<>(json, headers), Map.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                // After a successful trial the circuit should close
                closed = true;
                break;
            }
            Thread.sleep(200);
        }

        assertThat(closed).isTrue();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
