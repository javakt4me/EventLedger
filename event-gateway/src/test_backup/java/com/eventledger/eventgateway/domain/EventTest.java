package com.eventledger.eventgateway.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void testDefaultConstructor() {
        Event event = new Event();
        assertNull(event.getId());
        assertNull(event.getEventId());
        assertNull(event.getAccountId());
        assertNull(event.getType());
        assertNull(event.getAmount());
        assertNull(event.getCurrency());
        assertNull(event.getEventTimestamp());
        assertNull(event.getReceivedAt());
        assertNull(event.getMetadata());
        assertNull(event.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        Instant now = Instant.now();
        Event event = new Event(
                "evt-1", "acct-1", "CREDIT",
                new BigDecimal("100.00"), "USD", now, "{\"key\":\"val\"}"
        );

        assertEquals("evt-1", event.getEventId());
        assertEquals("acct-1", event.getAccountId());
        assertEquals("CREDIT", event.getType());
        assertEquals(new BigDecimal("100.00"), event.getAmount());
        assertEquals("USD", event.getCurrency());
        assertEquals(now, event.getEventTimestamp());
        assertNotNull(event.getReceivedAt());
        assertEquals("{\"key\":\"val\"}", event.getMetadata());
        assertEquals(EventStatus.PENDING, event.getStatus());
    }

    @Test
    void testSettersAndGetters() {
        Event event = new Event();
        Instant now = Instant.now();
        Instant received = Instant.now();

        event.setId(1L);
        event.setEventId("evt-set");
        event.setAccountId("acct-set");
        event.setType("DEBIT");
        event.setAmount(new BigDecimal("250.00"));
        event.setCurrency("EUR");
        event.setEventTimestamp(now);
        event.setReceivedAt(received);
        event.setMetadata("{\"source\":\"test\"}");
        event.setStatus(EventStatus.PROCESSED);

        assertEquals(1L, event.getId());
        assertEquals("evt-set", event.getEventId());
        assertEquals("acct-set", event.getAccountId());
        assertEquals("DEBIT", event.getType());
        assertEquals(new BigDecimal("250.00"), event.getAmount());
        assertEquals("EUR", event.getCurrency());
        assertEquals(now, event.getEventTimestamp());
        assertEquals(received, event.getReceivedAt());
        assertEquals("{\"source\":\"test\"}", event.getMetadata());
        assertEquals(EventStatus.PROCESSED, event.getStatus());
    }

    @Test
    void testEventStatusValues() {
        EventStatus[] values = EventStatus.values();
        assertEquals(3, values.length);
        assertEquals(EventStatus.PENDING, EventStatus.valueOf("PENDING"));
        assertEquals(EventStatus.PROCESSED, EventStatus.valueOf("PROCESSED"));
        assertEquals(EventStatus.FAILED, EventStatus.valueOf("FAILED"));
    }

    @Test
    void testStatusTransitions() {
        Event event = new Event("evt-1", "acct-1", "CREDIT",
                new BigDecimal("100"), "USD", Instant.now(), null);

        assertEquals(EventStatus.PENDING, event.getStatus());

        event.setStatus(EventStatus.PROCESSED);
        assertEquals(EventStatus.PROCESSED, event.getStatus());

        event.setStatus(EventStatus.FAILED);
        assertEquals(EventStatus.FAILED, event.getStatus());
    }

    @Test
    void testNullMetadata() {
        Event event = new Event("evt-1", "acct-1", "CREDIT",
                new BigDecimal("100"), "USD", Instant.now(), null);
        assertNull(event.getMetadata());
    }
}

