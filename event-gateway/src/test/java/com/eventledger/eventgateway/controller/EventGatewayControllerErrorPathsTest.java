package com.eventledger.eventgateway.controller;

import com.eventledger.eventgateway.client.AccountServiceClient;
import com.eventledger.eventgateway.dto.ErrorResponse;
import com.eventledger.eventgateway.dto.EventRequest;
import com.eventledger.eventgateway.dto.EventResponse;
import com.eventledger.eventgateway.metrics.EventGatewayMetrics;
import com.eventledger.eventgateway.service.EventGatewayService;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventGatewayControllerErrorPathsTest {

    @Mock
    private EventGatewayService eventGatewayService;

    @Mock
    private EventGatewayMetrics metrics;

    @InjectMocks
    private EventGatewayController eventGatewayController;

    private EventRequest eventRequest;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        eventRequest = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                new HashMap<>()
        );

        // Setup metrics mocks
        doNothing().when(metrics).incrementActiveEvents();
        doNothing().when(metrics).decrementActiveEvents();
        doNothing().when(metrics).recordEventCreated();
        doNothing().when(metrics).recordEventRetrieval();
        doNothing().when(metrics).recordEventError();
        doNothing().when(metrics).recordDownstreamError();
        when(metrics.startEventCreationTimer()).thenReturn(Timer.start());
        when(metrics.startEventRetrievalTimer()).thenReturn(Timer.start());
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(metrics.eventCreationTimer()).thenReturn(registry.timer("test"));
        when(metrics.eventRetrievalTimer()).thenReturn(registry.timer("test"));
    }

    @Test
    void testCreateEventWithServiceUnavailableException() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new AccountServiceClient.ServiceUnavailableException("Account service down"));

        ResponseEntity<?> response = eventGatewayController.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("SERVICE_UNAVAILABLE", errorResponse.getError());
        assertEquals(503, errorResponse.getStatus());
        verify(metrics, times(1)).recordDownstreamError();
    }

    @Test
    void testCreateEventWithGeneralException() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<?> response = eventGatewayController.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("INTERNAL_ERROR", errorResponse.getError());
        assertEquals(500, errorResponse.getStatus());
        verify(metrics, times(1)).recordEventError();
    }

    @Test
    void testCreateEventWithValidationError() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid event data"));

        ResponseEntity<?> response = eventGatewayController.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("VALIDATION_ERROR", errorResponse.getError());
        assertEquals(400, errorResponse.getStatus());
        verify(metrics, times(1)).recordEventError();
    }

    @Test
    void testGetEventNotFound() {
        when(eventGatewayService.getEvent("evt-nonexistent"))
                .thenThrow(new IllegalArgumentException("Event not found"));

        ResponseEntity<?> response = eventGatewayController.getEvent("evt-nonexistent");

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("NOT_FOUND", errorResponse.getError());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void testGetEventWithInternalError() {
        when(eventGatewayService.getEvent("evt-001"))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = eventGatewayController.getEvent("evt-001");

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    void testGetEventsByAccountWithException() {
        when(eventGatewayService.getEventsByAccount("acct-123"))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> eventGatewayController.getEventsByAccount("acct-123"));
    }

    @Test
    void testGetEventsByAccountSuccess() {
        List<EventResponse> mockEvents = new ArrayList<>();
        mockEvents.add(new EventResponse(
                "evt-001", "acct-123", "CREDIT", new BigDecimal("100.00"), "USD",
                now, now, "PROCESSED", "{}"
        ));

        when(eventGatewayService.getEventsByAccount("acct-123")).thenReturn(mockEvents);

        ResponseEntity<List<EventResponse>> response = eventGatewayController.getEventsByAccount("acct-123");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetEventsByAccountEmpty() {
        when(eventGatewayService.getEventsByAccount("acct-empty"))
                .thenReturn(new ArrayList<>());

        ResponseEntity<List<EventResponse>> response = eventGatewayController.getEventsByAccount("acct-empty");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void testGetEventsByAccountWithNullParameter() {
        List<EventResponse> mockEvents = new ArrayList<>();
        when(eventGatewayService.getEventsByAccount("")).thenReturn(mockEvents);

        ResponseEntity<List<EventResponse>> response = eventGatewayController.getEventsByAccount(null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testCreateEventMetricsCallsRecorded() {
        // Test that metrics methods are called appropriately
        EventResponse response = new EventResponse();
        response.setCreated(true);
        when(eventGatewayService.createEvent(any(EventRequest.class))).thenReturn(response);

        ResponseEntity<?> result = eventGatewayController.createEvent(eventRequest);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(metrics, times(1)).recordEventCreated();
    }

    @Test
    void testCreateEventSuccessWithMetrics() {
        EventResponse response = new EventResponse();
        response.setCreated(true);
        when(eventGatewayService.createEvent(any(EventRequest.class))).thenReturn(response);

        ResponseEntity<?> result = eventGatewayController.createEvent(eventRequest);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void testCreateEventErrorPaths() {
        // Test different exception types in sequence
        EventRequest req1 = new EventRequest("evt-1", "acct-1", "CREDIT", new BigDecimal("100"), "USD", now, new HashMap<>());
        EventRequest req2 = new EventRequest("evt-2", "acct-2", "CREDIT", new BigDecimal("100"), "USD", now, new HashMap<>());
        
        when(eventGatewayService.createEvent(req1))
                .thenThrow(new IllegalArgumentException("Invalid"));
        when(eventGatewayService.createEvent(req2))
                .thenThrow(new AccountServiceClient.ServiceUnavailableException("Down"));

        ResponseEntity<?> resp1 = eventGatewayController.createEvent(req1);
        ResponseEntity<?> resp2 = eventGatewayController.createEvent(req2);

        assertEquals(HttpStatus.BAD_REQUEST, resp1.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp2.getStatusCode());
    }
}
