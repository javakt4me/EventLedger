package com.eventledger.eventgateway.util;

import com.eventledger.eventgateway.dto.EventRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void testValidEventPasses() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertNull(error);
    }

    @Test
    void testMissingEventId() {
        EventRequest request = new EventRequest(
                null,
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Event ID is required", error);
    }

    @Test
    void testBlankEventId() {
        EventRequest request = new EventRequest(
                "",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Event ID is required", error);
    }

    @Test
    void testMissingAccountId() {
        EventRequest request = new EventRequest(
                "evt-001",
                null,
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Account ID is required", error);
    }

    @Test
    void testBlankAccountId() {
        EventRequest request = new EventRequest(
                "evt-001",
                "",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Account ID is required", error);
    }

    @Test
    void testMissingType() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                null,
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Type is required", error);
    }

    @Test
    void testBlankType() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Type is required", error);
    }

    @Test
    void testInvalidType() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "TRANSFER",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Type must be CREDIT or DEBIT", error);
    }

    @Test
    void testMissingAmount() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                null,
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Amount is required", error);
    }

    @Test
    void testNegativeAmount() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("-150.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Amount must be greater than 0", error);
    }

    @Test
    void testZeroAmount() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                BigDecimal.ZERO,
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Amount must be greater than 0", error);
    }

    @Test
    void testMissingCurrency() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                null,
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Currency is required", error);
    }

    @Test
    void testBlankCurrency() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "",
                Instant.now(),
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Currency is required", error);
    }

    @Test
    void testMissingEventTimestamp() {
        EventRequest request = new EventRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                null,
                new HashMap<>()
        );

        String error = ValidationUtil.validateEvent(request);
        assertEquals("Event timestamp is required", error);
    }
}

