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
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventGatewayServiceErrorPathsTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventGatewayService eventGatewayService;

    private EventRequest eventRequest;
    private Event failedEvent;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        eventRequest = new EventRequest(
                "evt-failed-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                new HashMap<>()
        );

        failedEvent = new Event(
                "evt-failed-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                "{}"
        );
        failedEvent.setId(1L);
        failedEvent.setReceivedAt(now);
        failedEvent.setStatus(EventStatus.FAILED);
    }

    @Test
    void testCreateEventReprocessFailedEventSuccess() throws Exception {
        // Test the path where a FAILED event is reprocessed and succeeds
        Event processedEvent = new Event(
                "evt-failed-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                "{}"
        );
        processedEvent.setId(1L);
        processedEvent.setReceivedAt(now);
        processedEvent.setStatus(EventStatus.PROCESSED);

        when(eventRepository.findByEventId("evt-failed-001")).thenReturn(Optional.of(failedEvent));
        doNothing().when(accountServiceClient).processTransaction(any(EventRequest.class));
        when(eventRepository.save(any(Event.class))).thenReturn(processedEvent);

        EventResponse response = eventGatewayService.createEvent(eventRequest);

        assertNotNull(response);
        assertEquals("evt-failed-001", response.getEventId());
        assertFalse(response.isCreated(), "Reprocessed event should have created=false");
        verify(accountServiceClient, times(1)).processTransaction(any(EventRequest.class));
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void testCreateEventReprocessFailedEventThrowsServiceUnavailable() throws Exception {
        // Test the path where a FAILED event is reprocessed but service is still unavailable
        when(eventRepository.findByEventId("evt-failed-001")).thenReturn(Optional.of(failedEvent));
        doThrow(new AccountServiceClient.ServiceUnavailableException("Service down"))
                .when(accountServiceClient).processTransaction(any(EventRequest.class));

        assertThrows(AccountServiceClient.ServiceUnavailableException.class,
                () -> eventGatewayService.createEvent(eventRequest));

        // Verify the event status is saved as FAILED
        verify(eventRepository, times(1)).save(argThat(e -> e.getStatus() == EventStatus.FAILED));
    }

    @Test
    void testCreateEventIdempotentProcessedEvent() throws Exception {
        // Test the path where an event was already PROCESSED (not FAILED)
        // Should return immediately with created=false without reprocessing
        Event processedEvent = new Event(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                "{}"
        );
        processedEvent.setId(1L);
        processedEvent.setReceivedAt(now);
        processedEvent.setStatus(EventStatus.PROCESSED);

        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                new HashMap<>()
        );

        when(eventRepository.findByEventId("evt-001")).thenReturn(Optional.of(processedEvent));

        EventResponse response = eventGatewayService.createEvent(request);

        assertNotNull(response);
        assertEquals("evt-001", response.getEventId());
        assertFalse(response.isCreated(), "Existing processed event should have created=false");
        verify(accountServiceClient, never()).processTransaction(any(EventRequest.class));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testCreateEventWithComplexMetadata() throws Exception {
        // Test metadata serialization with complex objects
        HashMap<String, Object> complexMetadata = new HashMap<>();
        complexMetadata.put("nested", new HashMap<String, String>() {{
            put("key", "value");
        }});

        EventRequest request = new EventRequest(
                "evt-complex",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                complexMetadata
        );

        Event event = new Event(
                "evt-complex",
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

        when(eventRepository.findByEventId("evt-complex")).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(complexMetadata)).thenReturn("{\"nested\":{\"key\":\"value\"}}");
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        doNothing().when(accountServiceClient).processTransaction(any(EventRequest.class));

        EventResponse response = eventGatewayService.createEvent(request);

        assertNotNull(response);
        assertTrue(response.isCreated(), "New event should have created=true");
    }

    @Test
    void testGetEventWithVariousEventStatuses() {
        // Test retrieving events with different statuses
        for (EventStatus status : EventStatus.values()) {
            Event event = new Event(
                    "evt-" + status.name(),
                    "acct-123",
                    "CREDIT",
                    new BigDecimal("100.00"),
                    "USD",
                    now,
                    "{}"
            );
            event.setStatus(status);
            event.setReceivedAt(now);

            when(eventRepository.findByEventId("evt-" + status.name())).thenReturn(Optional.of(event));

            EventResponse response = eventGatewayService.getEvent("evt-" + status.name());

            assertNotNull(response);
            assertEquals(status.toString(), response.getStatus());
        }
    }
}
