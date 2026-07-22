package com.eventledger.accountservice.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testDefaultConstructor() {
        Transaction tx = new Transaction();
        assertNull(tx.getId());
        assertNull(tx.getEventId());
        assertNull(tx.getAccountId());
        assertNull(tx.getType());
        assertNull(tx.getAmount());
        assertNull(tx.getCurrency());
        assertNull(tx.getEventTimestamp());
        assertNull(tx.getCreatedAt());
        assertNull(tx.getMetadata());
    }

    @Test
    void testAllArgsConstructor() {
        Instant now = Instant.now();
        Transaction tx = new Transaction(
                "evt-1", "acct-1", TransactionType.CREDIT,
                new BigDecimal("100.00"), "USD", now, "{\"key\":\"val\"}"
        );

        assertEquals("evt-1", tx.getEventId());
        assertEquals("acct-1", tx.getAccountId());
        assertEquals(TransactionType.CREDIT, tx.getType());
        assertEquals(new BigDecimal("100.00"), tx.getAmount());
        assertEquals("USD", tx.getCurrency());
        assertEquals(now, tx.getEventTimestamp());
        assertNotNull(tx.getCreatedAt());
        assertEquals("{\"key\":\"val\"}", tx.getMetadata());
    }

    @Test
    void testSettersAndGetters() {
        Transaction tx = new Transaction();
        Instant now = Instant.now();
        Instant created = Instant.now();

        tx.setId(1L);
        tx.setEventId("evt-set");
        tx.setAccountId("acct-set");
        tx.setType(TransactionType.DEBIT);
        tx.setAmount(new BigDecimal("250.00"));
        tx.setCurrency("EUR");
        tx.setEventTimestamp(now);
        tx.setCreatedAt(created);
        tx.setMetadata("{\"source\":\"test\"}");

        assertEquals(1L, tx.getId());
        assertEquals("evt-set", tx.getEventId());
        assertEquals("acct-set", tx.getAccountId());
        assertEquals(TransactionType.DEBIT, tx.getType());
        assertEquals(new BigDecimal("250.00"), tx.getAmount());
        assertEquals("EUR", tx.getCurrency());
        assertEquals(now, tx.getEventTimestamp());
        assertEquals(created, tx.getCreatedAt());
        assertEquals("{\"source\":\"test\"}", tx.getMetadata());
    }

    @Test
    void testTransactionTypeValues() {
        TransactionType[] values = TransactionType.values();
        assertEquals(2, values.length);
        assertEquals(TransactionType.CREDIT, TransactionType.valueOf("CREDIT"));
        assertEquals(TransactionType.DEBIT, TransactionType.valueOf("DEBIT"));
    }
}

