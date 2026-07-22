package com.eventledger.eventgateway.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    // === EventRequest Tests ===

    @Test
    void testEventRequestDefaultConstructor() {
        EventRequest req = new EventRequest();
        assertNull(req.getEventId());
        assertNull(req.getAccountId());
        assertNull(req.getType());
        assertNull(req.getAmount());
        assertNull(req.getCurrency());
        assertNull(req.getEventTimestamp());
        assertNull(req.getMetadata());
    }

    @Test
    void testEventRequestSettersAndGetters() {
        EventRequest req = new EventRequest();
        Instant now = Instant.now();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");

        req.setEventId("evt-1");
        req.setAccountId("acct-1");
        req.setType("CREDIT");
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setEventTimestamp(now);
        req.setMetadata(metadata);

        assertEquals("evt-1", req.getEventId());
        assertEquals("acct-1", req.getAccountId());
        assertEquals("CREDIT", req.getType());
        assertEquals(new BigDecimal("100.00"), req.getAmount());
        assertEquals("USD", req.getCurrency());
        assertEquals(now, req.getEventTimestamp());
        assertEquals(metadata, req.getMetadata());
        assertEquals("test", req.getMetadata().get("source"));
    }

    @Test
    void testEventRequestAllArgsConstructor() {
        Instant now = Instant.now();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("batch", "B-001");

        EventRequest req = new EventRequest(
                "evt-2", "acct-2", "DEBIT",
                new BigDecimal("200.00"), "EUR", now, metadata
        );

        assertEquals("evt-2", req.getEventId());
        assertEquals("acct-2", req.getAccountId());
        assertEquals("DEBIT", req.getType());
        assertEquals(new BigDecimal("200.00"), req.getAmount());
        assertEquals("EUR", req.getCurrency());
        assertEquals(now, req.getEventTimestamp());
        assertNotNull(req.getMetadata());
        assertEquals("B-001", req.getMetadata().get("batch"));
    }

    @Test
    void testEventRequestNullMetadata() {
        EventRequest req = new EventRequest("evt-3", "acct-3", "CREDIT",
                new BigDecimal("50"), "USD", Instant.now(), null);
        assertNull(req.getMetadata());
    }

    // === EventResponse Tests ===

    @Test
    void testEventResponseDefaultConstructor() {
        EventResponse res = new EventResponse();
        assertNull(res.getEventId());
        assertNull(res.getAccountId());
        assertNull(res.getType());
        assertNull(res.getAmount());
        assertNull(res.getCurrency());
        assertNull(res.getEventTimestamp());
        assertNull(res.getReceivedAt());
        assertNull(res.getStatus());
        assertNull(res.getMetadata());
    }

    @Test
    void testEventResponseSettersAndGetters() {
        EventResponse res = new EventResponse();
        Instant now = Instant.now();
        Instant received = Instant.now();

        res.setEventId("evt-r1");
        res.setAccountId("acct-r1");
        res.setType("CREDIT");
        res.setAmount(new BigDecimal("500.00"));
        res.setCurrency("USD");
        res.setEventTimestamp(now);
        res.setReceivedAt(received);
        res.setStatus("PROCESSED");
        res.setMetadata("{\"key\":\"val\"}");

        assertEquals("evt-r1", res.getEventId());
        assertEquals("acct-r1", res.getAccountId());
        assertEquals("CREDIT", res.getType());
        assertEquals(new BigDecimal("500.00"), res.getAmount());
        assertEquals("USD", res.getCurrency());
        assertEquals(now, res.getEventTimestamp());
        assertEquals(received, res.getReceivedAt());
        assertEquals("PROCESSED", res.getStatus());
        assertEquals("{\"key\":\"val\"}", res.getMetadata());
    }

    @Test
    void testEventResponseAllArgsConstructor() {
        Instant now = Instant.now();
        Instant received = Instant.now();

        EventResponse res = new EventResponse(
                "evt-r2", "acct-r2", "DEBIT",
                new BigDecimal("300.00"), "GBP", now, received, "FAILED", "{}"
        );

        assertEquals("evt-r2", res.getEventId());
        assertEquals("acct-r2", res.getAccountId());
        assertEquals("DEBIT", res.getType());
        assertEquals(new BigDecimal("300.00"), res.getAmount());
        assertEquals("GBP", res.getCurrency());
        assertEquals(now, res.getEventTimestamp());
        assertEquals(received, res.getReceivedAt());
        assertEquals("FAILED", res.getStatus());
        assertEquals("{}", res.getMetadata());
    }

    // === ErrorResponse Tests ===

    @Test
    void testErrorResponseDefaultConstructor() {
        ErrorResponse err = new ErrorResponse();
        assertNull(err.getError());
        assertNull(err.getMessage());
        assertEquals(0, err.getStatus());
    }

    @Test
    void testErrorResponseSettersAndGetters() {
        ErrorResponse err = new ErrorResponse();
        err.setError("VALIDATION_ERROR");
        err.setMessage("eventId is required");
        err.setStatus(400);

        assertEquals("VALIDATION_ERROR", err.getError());
        assertEquals("eventId is required", err.getMessage());
        assertEquals(400, err.getStatus());
    }

    @Test
    void testErrorResponseAllArgsConstructor() {
        ErrorResponse err = new ErrorResponse("NOT_FOUND", "Event not found", 404);

        assertEquals("NOT_FOUND", err.getError());
        assertEquals("Event not found", err.getMessage());
        assertEquals(404, err.getStatus());
    }

    @Test
    void testErrorResponseServiceUnavailable() {
        ErrorResponse err = new ErrorResponse("SERVICE_UNAVAILABLE", "Account Service is down", 503);
        assertEquals(503, err.getStatus());
    }

    @Test
    void testErrorResponseInternalError() {
        ErrorResponse err = new ErrorResponse("INTERNAL_ERROR", "Unexpected error", 500);
        assertEquals(500, err.getStatus());
    }
}

