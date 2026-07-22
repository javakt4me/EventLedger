package com.eventledger.eventgateway.service;

import com.eventledger.eventgateway.client.AccountServiceClient;
import com.eventledger.eventgateway.domain.Event;
import com.eventledger.eventgateway.domain.EventStatus;
import com.eventledger.eventgateway.dto.EventRequest;
import com.eventledger.eventgateway.dto.EventResponse;
import com.eventledger.eventgateway.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventGatewayServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventGatewayService eventGatewayService;

    private EventRequest eventRequest;
    private Event event;
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

        event = new Event(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                "{}"
        );
        event.setId(1L);
        event.setReceivedAt(now);
        event.setStatus(EventStatus.PROCESSED);
    }

    @Test
    void testCreateEventNew() throws Exception {
        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        doNothing().when(accountServiceClient).processTransaction(any(EventRequest.class));

        EventResponse response = eventGatewayService.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals("evt-001", response.getEventId());
        assertEquals("acct-123", response.getAccountId());
        assertEquals("CREDIT", response.getType());
        verify(eventRepository, times(2)).save(any(Event.class));
        verify(accountServiceClient, times(1)).processTransaction(any(EventRequest.class));
    }

    @Test
    void testCreateEventIdempotency() throws Exception {
        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.of(event));

        EventResponse response = eventGatewayService.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals("evt-001", response.getEventId());
        verify(eventRepository, never()).save(any(Event.class));
        verify(accountServiceClient, never()).processTransaction(any(EventRequest.class));
    }

    @Test
    void testCreateEventValidationFailure() {
        eventRequest.setEventId(null);

        assertThrows(IllegalArgumentException.class, () -> eventGatewayService.createEvent(eventRequest));
    }

    @Test
    void testCreateEventWithInvalidAmount() {
        eventRequest.setAmount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> eventGatewayService.createEvent(eventRequest));
    }

    @Test
    void testCreateEventWithInvalidType() {
        eventRequest.setType("INVALID");

        assertThrows(IllegalArgumentException.class, () -> eventGatewayService.createEvent(eventRequest));
    }

    @Test
    void testCreateEventAccountServiceUnavailable() throws Exception {
        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        doThrow(new AccountServiceClient.ServiceUnavailableException("Service down"))
                .when(accountServiceClient).processTransaction(any(EventRequest.class));

        assertThrows(AccountServiceClient.ServiceUnavailableException.class,
                () -> eventGatewayService.createEvent(eventRequest));
    }

    @Test
    void testCreateEventMetadataSerializationError() throws Exception {
        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));

        assertThrows(RuntimeException.class, () -> eventGatewayService.createEvent(eventRequest));
    }

    @Test
    void testGetEventSuccess() {
        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.of(event));

        EventResponse response = eventGatewayService.getEvent("evt-001");

        assertNotNull(response);
        assertEquals("evt-001", response.getEventId());
        assertEquals("acct-123", response.getAccountId());
    }

    @Test
    void testGetEventNotFound() {
        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> eventGatewayService.getEvent("evt-001"));
    }

    @Test
    void testGetEventsByAccountSuccess() {
        List<Event> events = new ArrayList<>();
        events.add(event);

        when(eventRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(events);

        List<EventResponse> responses = eventGatewayService.getEventsByAccount("acct-123");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("evt-001", responses.get(0).getEventId());
    }

    @Test
    void testGetEventsByAccountEmpty() {
        List<Event> events = new ArrayList<>();

        when(eventRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(events);

        List<EventResponse> responses = eventGatewayService.getEventsByAccount("acct-123");

        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    void testGetEventsByAccountMultipleEvents() {
        Event event2 = new Event(
                "evt-002",
                "acct-123",
                "DEBIT",
                new BigDecimal("50.00"),
                "USD",
                Instant.now().plusSeconds(1),
                "{}"
        );
        event2.setId(2L);
        event2.setReceivedAt(Instant.now().plusSeconds(1));
        event2.setStatus(EventStatus.PROCESSED);

        List<Event> events = new ArrayList<>();
        events.add(event);
        events.add(event2);

        when(eventRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(events);

        List<EventResponse> responses = eventGatewayService.getEventsByAccount("acct-123");

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("evt-001", responses.get(0).getEventId());
        assertEquals("evt-002", responses.get(1).getEventId());
    }

    @Test
    void testCreateEventNullMetadata() throws Exception {
        eventRequest.setMetadata(null);
        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        doNothing().when(accountServiceClient).processTransaction(any(EventRequest.class));

        EventResponse response = eventGatewayService.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals("evt-001", response.getEventId());
        verify(eventRepository, times(2)).save(any(Event.class));
    }
}

