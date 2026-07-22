package com.eventledger.eventgateway.controller;

import com.eventledger.eventgateway.client.AccountServiceClient;
import com.eventledger.eventgateway.dto.ErrorResponse;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventGatewayControllerTest {

    @Mock
    private EventGatewayService eventGatewayService;

    @Mock
    private EventGatewayMetrics metrics;

    @InjectMocks
    private EventGatewayController eventGatewayController;

    private EventRequest eventRequest;
    private EventResponse eventResponse;
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

        eventResponse = new EventResponse(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                now,
                "PROCESSED",
                "{}"
        );

        // Setup metrics mocks
        doNothing().when(metrics).incrementActiveEvents();
        doNothing().when(metrics).decrementActiveEvents();
        doNothing().when(metrics).recordEventCreated();
        doNothing().when(metrics).recordEventRetrieval();
        doNothing().when(metrics).recordEventError();
        doNothing().when(metrics).recordDownstreamError();
        when(metrics.startEventCreationTimer()).thenReturn(io.micrometer.core.instrument.Timer.start());
        when(metrics.startEventRetrievalTimer()).thenReturn(io.micrometer.core.instrument.Timer.start());
        when(metrics.eventCreationTimer()).thenReturn(io.micrometer.core.instrument.Timer.builder("test").register(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        when(metrics.eventRetrievalTimer()).thenReturn(io.micrometer.core.instrument.Timer.builder("test").register(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    }

    @Test
    void testCreateEventSuccess() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenReturn(eventResponse);

        ResponseEntity<?> response = eventGatewayController.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(eventResponse, response.getBody());
        verify(eventGatewayService, times(1)).createEvent(any(EventRequest.class));
    }

    @Test
    void testCreateEventValidationError() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid event"));

        ResponseEntity<?> response = eventGatewayController.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("VALIDATION_ERROR", error.getError());
        assertEquals(400, error.getStatus());
    }

    @Test
    void testCreateEventServiceUnavailable() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new AccountServiceClient.ServiceUnavailableException("Service down"));

        ResponseEntity<?> response = eventGatewayController.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("SERVICE_UNAVAILABLE", error.getError());
        assertEquals(503, error.getStatus());
    }

    @Test
    void testCreateEventInternalError() {
        when(eventGatewayService.createEvent(any(EventRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<?> response = eventGatewayController.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("INTERNAL_ERROR", error.getError());
        assertEquals(500, error.getStatus());
    }

    @Test
    void testGetEventSuccess() {
        when(eventGatewayService.getEvent("evt-001"))
                .thenReturn(eventResponse);

        ResponseEntity<?> response = eventGatewayController.getEvent("evt-001");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(eventResponse, response.getBody());
        verify(eventGatewayService, times(1)).getEvent("evt-001");
    }

    @Test
    void testGetEventNotFound() {
        when(eventGatewayService.getEvent("evt-001"))
                .thenThrow(new IllegalArgumentException("Event not found"));

        ResponseEntity<?> response = eventGatewayController.getEvent("evt-001");

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertInstanceOf(ErrorResponse.class, response.getBody());
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("NOT_FOUND", error.getError());
        assertEquals(404, error.getStatus());
    }

    @Test
    void testGetEventsByAccountSuccess() {
        List<EventResponse> eventResponses = new ArrayList<>();
        eventResponses.add(eventResponse);

        when(eventGatewayService.getEventsByAccount("acct-123"))
                .thenReturn(eventResponses);

        ResponseEntity<List<EventResponse>> response = eventGatewayController.getEventsByAccount("acct-123");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("evt-001", response.getBody().get(0).getEventId());
        verify(eventGatewayService, times(1)).getEventsByAccount("acct-123");
    }

    @Test
    void testGetEventsByAccountEmpty() {
        List<EventResponse> eventResponses = new ArrayList<>();

        when(eventGatewayService.getEventsByAccount("acct-123"))
                .thenReturn(eventResponses);

        ResponseEntity<List<EventResponse>> response = eventGatewayController.getEventsByAccount("acct-123");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
        verify(eventGatewayService, times(1)).getEventsByAccount("acct-123");
    }

}

