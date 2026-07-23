package com.eventledger.eventgateway.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EventResponseConstructorTest {

    @Test
    void testDefaultConstructor() {
        EventResponse response = new EventResponse();
        
        assertNotNull(response);
        assertFalse(response.isCreated());
    }

    @Test
    void testFullConstructorWithReceivedAt() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                now,
                "PROCESSED",
                "{}"
        );

        assertNotNull(response);
        assertEquals("evt-123", response.getEventId());
        assertEquals("acct-456", response.getAccountId());
        assertEquals("CREDIT", response.getType());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals(now, response.getEventTimestamp());
        assertEquals(now, response.getReceivedAt());
        assertEquals("PROCESSED", response.getStatus());
        assertEquals("{}", response.getMetadata());
        // Constructor should set created=true when receivedAt is not null
        assertTrue(response.isCreated());
    }

    @Test
    void testFullConstructorWithNullReceivedAtButStatus() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                null,
                "PENDING",
                "{}"
        );

        assertNotNull(response);
        assertNull(response.getReceivedAt());
        assertEquals("PENDING", response.getStatus());
        // Constructor should set created=true when status is non-empty
        assertTrue(response.isCreated());
    }

    @Test
    void testFullConstructorWithNullReceivedAtAndNullStatus() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                null,
                null,
                "{}"
        );

        assertNotNull(response);
        assertNull(response.getReceivedAt());
        assertNull(response.getStatus());
        // Constructor should set created=false when both receivedAt and status are null
        assertFalse(response.isCreated());
    }

    @Test
    void testFullConstructorWithNullReceivedAtAndEmptyStatus() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                null,
                "",
                "{}"
        );

        assertNotNull(response);
        assertNull(response.getReceivedAt());
        assertEquals("", response.getStatus());
        // Constructor should set created=false when status is empty
        assertFalse(response.isCreated());
    }

    @Test
    void testFullConstructorWithReceivedAtAndNullStatus() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                now,
                null,
                "{}"
        );

        assertNotNull(response);
        assertEquals(now, response.getReceivedAt());
        assertNull(response.getStatus());
        // Constructor should set created=true when receivedAt is not null
        assertTrue(response.isCreated());
    }

    @Test
    void testFullConstructorWithReceivedAtAndEmptyStatus() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                now,
                "",
                "{}"
        );

        assertNotNull(response);
        assertEquals(now, response.getReceivedAt());
        assertEquals("", response.getStatus());
        // Constructor should set created=true when receivedAt is not null
        assertTrue(response.isCreated());
    }

    @Test
    void testSetCreatedOverridesConstructor() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now,
                now,
                "PROCESSED",
                "{}"
        );

        // Constructor set created=true
        assertTrue(response.isCreated());
        
        // setCreated should override
        response.setCreated(false);
        assertFalse(response.isCreated());
    }

    @Test
    void testGettersAndSetters() {
        EventResponse response = new EventResponse();
        Instant now = Instant.now();
        
        response.setEventId("evt-123");
        response.setAccountId("acct-456");
        response.setType("CREDIT");
        response.setAmount(new BigDecimal("100.00"));
        response.setCurrency("USD");
        response.setEventTimestamp(now);
        response.setReceivedAt(now);
        response.setStatus("PROCESSED");
        response.setMetadata("{}");
        response.setCreated(true);
        
        assertEquals("evt-123", response.getEventId());
        assertEquals("acct-456", response.getAccountId());
        assertEquals("CREDIT", response.getType());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals(now, response.getEventTimestamp());
        assertEquals(now, response.getReceivedAt());
        assertEquals("PROCESSED", response.getStatus());
        assertEquals("{}", response.getMetadata());
        assertTrue(response.isCreated());
    }

    @Test
    void testConstructorWithAllNullValues() {
        EventResponse response = new EventResponse(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNotNull(response);
        assertNull(response.getEventId());
        assertNull(response.getAccountId());
        assertNull(response.getType());
        assertNull(response.getAmount());
        assertNull(response.getCurrency());
        assertNull(response.getEventTimestamp());
        assertNull(response.getReceivedAt());
        assertNull(response.getStatus());
        assertNull(response.getMetadata());
        assertFalse(response.isCreated());
    }

    @Test
    void testConstructorWithNullEventTimestamp() {
        // eventTimestamp can be null even though receivedAt is set
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                null,
                now,
                "PROCESSED",
                "{}"
        );

        assertNotNull(response);
        assertNull(response.getEventTimestamp());
        assertEquals(now, response.getReceivedAt());
        assertTrue(response.isCreated());
    }

    @Test
    void testConstructorWithNullAmount() {
        Instant now = Instant.now();
        EventResponse response = new EventResponse(
                "evt-123",
                "acct-456",
                "CREDIT",
                null,
                "USD",
                now,
                now,
                "PROCESSED",
                "{}"
        );

        assertNotNull(response);
        assertNull(response.getAmount());
        assertTrue(response.isCreated());
    }

    @Test
    void testMultipleCreatedToggle() {
        EventResponse response = new EventResponse();
        
        assertFalse(response.isCreated());
        response.setCreated(true);
        assertTrue(response.isCreated());
        response.setCreated(false);
        assertFalse(response.isCreated());
        response.setCreated(true);
        assertTrue(response.isCreated());
    }
}
