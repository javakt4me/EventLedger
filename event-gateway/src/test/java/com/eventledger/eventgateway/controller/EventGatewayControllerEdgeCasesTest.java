package com.eventledger.eventgateway.controller;

import com.eventledger.eventgateway.client.AccountServiceClient;
import com.eventledger.eventgateway.dto.EventRequest;
import com.eventledger.eventgateway.dto.EventResponse;
import com.eventledger.eventgateway.metrics.EventGatewayMetrics;
import com.eventledger.eventgateway.service.EventGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventGatewayControllerEdgeCasesTest {

    @Mock
    private EventGatewayService eventGatewayService;

    @Mock
    private EventGatewayMetrics metrics;

    @InjectMocks
    private EventGatewayController controller;

    private EventRequest eventRequest;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        eventRequest = new EventRequest(
                "evt-edge-1",
                "acct-edge",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                new HashMap<>()
        );

        eventResponse = new EventResponse(
                "evt-edge-1",
                "acct-edge",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                now,
                "PROCESSED",
                "{}"
        );
    }

    @Test
    void testCreateEventSuccess() {
        eventResponse.setCreated(true);
        when(eventGatewayService.createEvent(any(EventRequest.class))).thenReturn(eventResponse);

        ResponseEntity<?> resp = controller.createEvent(eventRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo(eventResponse);
    }

    @Test
    void testCreateEventIdempotent() {
        eventResponse.setCreated(false);
        when(eventGatewayService.createEvent(any(EventRequest.class))).thenReturn(eventResponse);

        ResponseEntity<?> resp = controller.createEvent(eventRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(eventResponse);
    }

    @Test
    void testCreateEventValidationError() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid event"));

        ResponseEntity<?> resp = controller.createEvent(eventRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testCreateEventServiceUnavailable() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new AccountServiceClient.ServiceUnavailableException("Service down"));

        ResponseEntity<?> resp = controller.createEvent(eventRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void testCreateEventInternalError() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> resp = controller.createEvent(eventRequest);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testGetEventSuccess() {
        when(eventGatewayService.getEvent("evt-edge-1")).thenReturn(eventResponse);

        ResponseEntity<?> resp = controller.getEvent("evt-edge-1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(eventResponse);
    }

    @Test
    void testGetEventNotFound() {
        when(eventGatewayService.getEvent("evt-missing"))
                .thenThrow(new IllegalArgumentException("Event not found"));

        ResponseEntity<?> resp = controller.getEvent("evt-missing");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetEventInternalError() {
        when(eventGatewayService.getEvent("evt-error"))
                .thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> resp = controller.getEvent("evt-error");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testGetEventsByAccountWithParam() {
        when(eventGatewayService.getEventsByAccount("acct-edge"))
                .thenReturn(java.util.Arrays.asList(eventResponse));

        ResponseEntity<?> resp = controller.getEventsByAccount("acct-edge");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetEventsByAccountEmpty() {
        when(eventGatewayService.getEventsByAccount("acct-empty"))
                .thenReturn(java.util.Collections.emptyList());

        ResponseEntity<?> resp = controller.getEventsByAccount("acct-empty");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetEventsByAccountNull() {
        when(eventGatewayService.getEventsByAccount(""))
                .thenReturn(java.util.Collections.emptyList());

        ResponseEntity<?> resp = controller.getEventsByAccount(null);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
