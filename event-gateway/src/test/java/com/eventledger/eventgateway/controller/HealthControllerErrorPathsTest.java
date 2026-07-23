package com.eventledger.eventgateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthControllerErrorPathsTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private RestTemplate restTemplate;

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController(dataSource, restTemplate, "http://localhost:8081");
    }

    @Test
    void testHealthCheckWithDatabaseFailure() throws SQLException {
        // Test the database error path
        when(dataSource.getConnection()).thenThrow(new SQLException("Database connection failed"));

        ResponseEntity<?> response = healthController.health();

        assertNotNull(response);
        // The health endpoint should still return a response even if DB fails
        // It may return degraded status or include error info
    }

    @Test
    void testHealthCheckWithDatabaseConnectionException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection pool exhausted"));

        ResponseEntity<?> response = healthController.health();
        assertNotNull(response);
    }

    @Test
    void testHealthCheckWithConnectionNull() throws SQLException {
        // This tests the path where connection returns null
        when(dataSource.getConnection()).thenReturn(null);

        ResponseEntity<?> response = healthController.health();
        assertNotNull(response);
    }

    @Test
    void testHealthCheckWithDownstreamServiceFailure() {
        when(restTemplate.getForObject("http://localhost:8081/accounts/health", Map.class))
                .thenThrow(new RuntimeException("Downstream service unreachable"));

        ResponseEntity<?> response = healthController.health();

        assertNotNull(response);
        // Health endpoint should handle downstream failures gracefully
    }

    @Test
    void testHealthCheckWithBothServicesDown() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB down"));
        when(restTemplate.getForObject("http://localhost:8081/accounts/health", Map.class))
                .thenThrow(new RuntimeException("Service down"));

        ResponseEntity<?> response = healthController.health();

        assertNotNull(response);
        // Should return appropriate response indicating multiple failures
    }

    @Test
    void testCheckDatabaseHealthWithException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        assertDoesNotThrow(() -> {
            // The method should not throw even if DB check fails
            healthController.health();
        });
    }

    @Test
    void testHealthCheckResponseStructure() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(restTemplate.getForObject("http://localhost:8081/accounts/health", Map.class))
                .thenReturn(Map.of("status", "UP"));

        ResponseEntity<?> response = healthController.health();

        assertNotNull(response);
        assertNotNull(response.getBody());
    }

    @Test
    void testHealthCheckWithValidConnection() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(restTemplate.getForObject("http://localhost:8081/accounts/health", Map.class))
                .thenReturn(Map.of("status", "UP"));

        ResponseEntity<?> response = healthController.health();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mockConnection, times(1)).close();
    }

    @Test
    void testHealthCheckDownstreamResponseNull() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(restTemplate.getForObject("http://localhost:8081/accounts/health", Map.class))
                .thenReturn(null);

        ResponseEntity<?> response = healthController.health();

        assertNotNull(response);
    }

    @Test
    void testHealthCheckConnectionCloseFailure() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();
        when(restTemplate.getForObject("http://localhost:8081/accounts/health", Map.class))
                .thenReturn(Map.of("status", "UP"));

        ResponseEntity<?> response = healthController.health();

        assertNotNull(response);
        // Should handle close failure gracefully
    }

    @Test
    void testHealthCheckMultipleCalls() throws SQLException {
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(restTemplate.getForObject("http://localhost:8081/accounts/health", Map.class))
                .thenReturn(Map.of("status", "UP"));

        // Make multiple calls to ensure no state issues
        ResponseEntity<?> response1 = healthController.health();
        ResponseEntity<?> response2 = healthController.health();

        assertNotNull(response1);
        assertNotNull(response2);
    }
}
