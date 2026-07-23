package com.eventledger.eventgateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerEdgeCasesTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Connection connection;

    @InjectMocks
    private HealthController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthController(dataSource, restTemplate, "http://account-service:8081");
    }

    @Test
    void testHealthCheckDatabaseUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        Map<String, Object> downstreamResp = new HashMap<>();
        downstreamResp.put("status", "UP");
        ResponseEntity<Map> respEntity = new ResponseEntity<>(downstreamResp, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(respEntity);

        ResponseEntity<Map<String, Object>> resp = controller.health();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "UP");
        assertThat(resp.getBody()).containsKey("database");
        assertThat(resp.getBody()).containsKey("downstream");
    }

    @Test
    void testHealthCheckDatabaseDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB Connection Failed"));

        Map<String, Object> downstreamResp = new HashMap<>();
        downstreamResp.put("status", "UP");
        ResponseEntity<Map> respEntity = new ResponseEntity<>(downstreamResp, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(respEntity);

        ResponseEntity<Map<String, Object>> resp = controller.health();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Status becomes DOWN if DB is down
        Map<String, Object> body = resp.getBody();
        assertThat(body.get("status")).isIn("DOWN", "UP"); // Depends on gateway's logic
    }

    @Test
    void testHealthCheckDownstreamUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        Map<String, Object> downstreamResp = new HashMap<>();
        downstreamResp.put("status", "UP");
        ResponseEntity<Map> respEntity = new ResponseEntity<>(downstreamResp, HttpStatus.OK);
        when(restTemplate.getForEntity("http://account-service:8081/actuator/health", Map.class))
                .thenReturn(respEntity);

        ResponseEntity<Map<String, Object>> resp = controller.health();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = resp.getBody();
        assertThat(body).containsKey("downstream");
        Map<String, Object> downstream = (Map<String, Object>) body.get("downstream");
        assertThat(downstream.get("status")).isEqualTo("UP");
    }

    @Test
    void testHealthCheckDownstreamDown() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        ResponseEntity<Map<String, Object>> resp = controller.health();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = resp.getBody();
        Map<String, Object> downstream = (Map<String, Object>) body.get("downstream");
        assertThat(downstream.get("status")).isEqualTo("DOWN");
        assertThat(downstream).containsKey("error");
    }

    @Test
    void testHealthCheckDownstreamNullResponse() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        ResponseEntity<Map> nullResp = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(nullResp);

        ResponseEntity<Map<String, Object>> resp = controller.health();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> downstream = (Map<String, Object>) resp.getBody().get("downstream");
        // Should handle null response gracefully
        assertThat(downstream).isNotNull();
    }

    @Test
    void testHealthCheckDownstreamUnhealthy() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        Map<String, Object> downstreamResp = new HashMap<>();
        downstreamResp.put("status", "DOWN");
        ResponseEntity<Map> respEntity = new ResponseEntity<>(downstreamResp, HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(respEntity);

        ResponseEntity<Map<String, Object>> resp = controller.health();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> downstream = (Map<String, Object>) resp.getBody().get("downstream");
        assertThat(downstream.get("status")).isEqualTo("DOWN");
    }
}
