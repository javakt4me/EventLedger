package com.eventledger.eventgateway.util;

import com.eventledger.eventgateway.dto.EventRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValidationUtilComprehensiveTest {

    @Test
    void testValidateEventWithAllValidFields() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNull(result, "Valid event should return null");
    }

    @Test
    void testValidateEventWithNullEventId() {
        EventRequest request = new EventRequest(
                null,
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Null eventId should return error message");
    }

    @Test
    void testValidateEventWithEmptyEventId() {
        EventRequest request = new EventRequest(
                "",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Empty eventId should return error message");
    }

    @Test
    void testValidateEventWithNullAccountId() {
        EventRequest request = new EventRequest(
                "evt-123",
                null,
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Null accountId should return error message");
    }

    @Test
    void testValidateEventWithEmptyAccountId() {
        EventRequest request = new EventRequest(
                "evt-123",
                "",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Empty accountId should return error message");
    }

    @Test
    void testValidateEventWithNullType() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                null,
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Null type should return error message");
    }

    @Test
    void testValidateEventWithEmptyType() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Empty type should return error message");
    }

    @Test
    void testValidateEventWithInvalidType() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "INVALID_TYPE",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Invalid type should return error message");
    }

    @Test
    void testValidateEventWithNullAmount() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                null,
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Null amount should return error message");
    }

    @Test
    void testValidateEventWithZeroAmount() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                BigDecimal.ZERO,
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Zero amount should return error message");
    }

    @Test
    void testValidateEventWithNegativeAmount() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("-100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Negative amount should return error message");
    }

    @Test
    void testValidateEventWithNullCurrency() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                null,
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Null currency should return error message");
    }

    @Test
    void testValidateEventWithEmptyCurrency() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Empty currency should return error message");
    }

    @Test
    void testValidateEventWithNullTimestamp() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                null,
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Null timestamp should return error message");
    }

    @Test
    void testValidateEventWithDebitType() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "DEBIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNull(result, "Valid DEBIT event should return null");
    }

    @Test
    void testValidateEventWithCreditType() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNull(result, "Valid CREDIT event should return null");
    }

    @Test
    void testValidateEventWithValidLongCurrency() {
        EventRequest request = new EventRequest(
                "evt-123",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "GBP",  // Valid 3-letter currency code
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNull(result, "Valid currency code should return null");
    }

    @Test
    void testValidateEventWithSpecialCharactersInEventId() {
        EventRequest request = new EventRequest(
                "evt-123abc",
                "acct-456",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        // Alphanumeric with dash should be valid
        assertNull(result);
    }

    @Test
    void testValidateEventMultipleErrors() {
        // Test with multiple invalid fields - should return first error
        EventRequest request = new EventRequest(
                null,
                null,
                null,
                BigDecimal.ZERO,
                null,
                null,
                new HashMap<>()
        );

        String result = ValidationUtil.validateEvent(request);
        assertNotNull(result, "Should return error for first invalid field");
    }
}
