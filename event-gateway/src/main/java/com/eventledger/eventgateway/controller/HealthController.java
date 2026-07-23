package com.eventledger.eventgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private final DataSource dataSource;
    private final RestTemplate restTemplate;
    private final String accountServiceUrl;

    public HealthController(DataSource dataSource, RestTemplate restTemplate,
                            @Value("${account.service.url:http://account-service:8081}") String accountServiceUrl) {
        this.dataSource = dataSource;
        this.restTemplate = restTemplate;
        this.accountServiceUrl = accountServiceUrl;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthResponse = new HashMap<>();
        String status = "UP";
        
        // Check database connectivity
        Map<String, String> dbStatus = checkDatabaseHealth();
        if ("DOWN".equals(dbStatus.get("status"))) {
            status = "DOWN";
        }
        
        healthResponse.put("status", status);
        healthResponse.put("service", "event-gateway");
        healthResponse.put("timestamp", System.currentTimeMillis());
        healthResponse.put("database", dbStatus);

        // Check downstream service availability (informational)
        Map<String, String> downstreamStatus = new HashMap<>();
        downstreamStatus.put("service", "account-service");
        String url = accountServiceUrl + "/actuator/health";
        downstreamStatus.put("url", url);
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object statusObj = resp.getBody().get("status");
                downstreamStatus.put("status", statusObj != null ? statusObj.toString() : "UP");
            } else {
                downstreamStatus.put("status", "DOWN");
            }
        } catch (Exception e) {
            downstreamStatus.put("status", "DOWN");
            downstreamStatus.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        healthResponse.put("downstream", downstreamStatus);

        logger.info("Health check performed - Status: {}, Database: {}", status, dbStatus.get("status"));
        return ResponseEntity.ok(healthResponse);
    }

    private Map<String, String> checkDatabaseHealth() {
        Map<String, String> dbStatus = new HashMap<>();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (connection != null && !connection.isClosed()) {
                dbStatus.put("status", "UP");
                dbStatus.put("connection", "OK");
            } else {
                dbStatus.put("status", "DOWN");
                dbStatus.put("connection", "FAILED");
            }
        } catch (Exception e) {
            dbStatus.put("status", "DOWN");
            dbStatus.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            logger.error("Database health check failed", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    logger.debug("Error closing database connection", e);
                }
            }
        }
        return dbStatus;
    }
}

