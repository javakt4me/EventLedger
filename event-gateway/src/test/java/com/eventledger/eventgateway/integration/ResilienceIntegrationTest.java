package com.eventledger.eventgateway.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResilienceIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
        // Ensure retry attempts for test
        registry.add("resilience4j.retry.instances.accountService.maxAttempts", () -> "3");
        registry.add("resilience4j.retry.instances.accountService.waitDuration", () -> "10ms");
    }

    @Test
    void testRetryThenSuccess() throws Exception {
        // Configure WireMock: first 2 calls fail (500), third succeeds (200)
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlPathMatching("/accounts/.*/transactions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second-failed"));

        wireMockServer.stubFor(post(urlPathMatching("/accounts/.*/transactions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("second-failed")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third-success"));

        wireMockServer.stubFor(post(urlPathMatching("/accounts/.*/transactions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("third-success")
                .willReturn(aResponse().withStatus(200)));

        String eventJson = "{\"eventId\":\"evt-res-1\",\"accountId\":\"acct-res\",\"type\":\"CREDIT\",\"amount\":100.00,\"currency\":\"USD\",\"eventTimestamp\":\"2020-01-01T00:00:00Z\",\"metadata\":{}}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        ResponseEntity<Map> resp = restTemplate.postForEntity("http://localhost:" + port + "/events", new HttpEntity<>(eventJson, headers), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify WireMock saw 3 calls
        int calls = wireMockServer.findAll(postRequestedFor(urlPathMatching("/accounts/.*/transactions"))).size();
        assertThat(calls).isEqualTo(3);
    }

    @Test
    void testAlwaysFailReturns503() throws Exception {
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlPathMatching("/accounts/.*/transactions"))
                .willReturn(aResponse().withStatus(500)));

        String eventJson = "{\"eventId\":\"evt-res-2\",\"accountId\":\"acct-res\",\"type\":\"CREDIT\",\"amount\":50.00,\"currency\":\"USD\",\"eventTimestamp\":\"2020-01-01T00:00:00Z\",\"metadata\":{}}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        ResponseEntity<Map> resp = restTemplate.postForEntity("http://localhost:" + port + "/events", new HttpEntity<>(eventJson, headers), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        // Retry attempts should have been made
        int calls = wireMockServer.findAll(postRequestedFor(urlPathMatching("/accounts/.*/transactions"))).size();
        assertThat(calls).isEqualTo(3);
    }
}
