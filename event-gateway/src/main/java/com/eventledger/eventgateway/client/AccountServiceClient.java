package com.eventledger.eventgateway.client;

import com.eventledger.eventgateway.dto.EventRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class AccountServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceClient.class);
    private final RestTemplate restTemplate;
    private static final String ACCOUNT_SERVICE_URL = "http://localhost:8081";

    public AccountServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "handleAccountServiceDown")
    @Retry(name = "accountService")
    public void processTransaction(EventRequest request) {
        String url = ACCOUNT_SERVICE_URL + "/accounts/" + request.getAccountId() + "/transactions";
        logger.debug("Calling Account Service at: {} - Transaction will be traced across services", url);
        try {
            restTemplate.postForObject(url, request, Object.class);
            logger.info("Successfully processed transaction for account: {}", request.getAccountId());
        } catch (Exception e) {
            logger.error("Error calling Account Service: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void handleAccountServiceDown(EventRequest request, Exception ex) {
        logger.error("Account Service fallback triggered for account: {}", request.getAccountId());
        throw new ServiceUnavailableException("Account Service is temporarily unavailable: " + ex.getMessage());
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
}

