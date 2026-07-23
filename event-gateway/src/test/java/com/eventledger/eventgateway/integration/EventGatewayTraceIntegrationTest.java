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

import java.util.List;
import java.util.Map;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EventGatewayTraceIntegrationTest {

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
        wireMockServer.stubFor(post(urlPathMatching("/accounts/.*/transactions"))
                .willReturn(aResponse().withStatus(200)));
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
    }

    @Test
    void testTraceHeaderPropagatedAndIdempotency() throws Exception {
        // Build a sample event JSON
        String eventJson = "{\"eventId\":\"evt-int-1\",\"accountId\":\"acct-42\",\"type\":\"CREDIT\",\"amount\":150.00,\"currency\":\"USD\",\"eventTimestamp\":\"2020-01-01T00:00:00Z\",\"metadata\":{}}";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        // First request - expect 201 Created
        ResponseEntity<Map> first = restTemplate.postForEntity("http://localhost:" + port + "/events", new HttpEntity<>(eventJson, headers), Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify WireMock received a request and that it contained tracing headers
        Thread.sleep(200); // small wait for async headers
        List<com.github.tomakehurst.wiremock.verification.LoggedRequest> reqs = wireMockServer.findAll(postRequestedFor(urlPathMatching("/accounts/.*/transactions")));
        assertThat(reqs).isNotEmpty();
        com.github.tomakehurst.wiremock.verification.LoggedRequest logged = reqs.get(0);
        // Assert at least one common trace header exists (W3C traceparent or B3)
        boolean hasTrace = logged.containsHeader("traceparent") || logged.containsHeader("b3") || logged.containsHeader("X-B3-TraceId") || logged.containsHeader("traceId");
        assertThat(hasTrace).isTrue();

        // Second request (duplicate) - expect 200 OK and created=false
        ResponseEntity<Map> second = restTemplate.postForEntity("http://localhost:" + port + "/events", new HttpEntity<>(eventJson, headers), Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object createdFlag = second.getBody().get("created");
        assertThat(createdFlag).isEqualTo(false);
    }
}
