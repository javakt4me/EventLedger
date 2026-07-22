package com.eventledger.eventgateway.client;

import com.eventledger.eventgateway.dto.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AccountServiceClient accountServiceClient;

    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        eventRequest = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );
    }

    @Test
    void testProcessTransactionSuccess() {
        when(restTemplate.postForObject(anyString(), any(EventRequest.class), any()))
                .thenReturn(new Object());

        assertDoesNotThrow(() -> accountServiceClient.processTransaction(eventRequest));

        verify(restTemplate, times(1)).postForObject(anyString(), any(EventRequest.class), any());
    }

    @Test
    void testHandleAccountServiceDown() {
        Exception exception = new RuntimeException("Connection failed");
        
        assertThrows(AccountServiceClient.ServiceUnavailableException.class, () -> {
            accountServiceClient.handleAccountServiceDown(eventRequest, exception);
        });
    }
}

