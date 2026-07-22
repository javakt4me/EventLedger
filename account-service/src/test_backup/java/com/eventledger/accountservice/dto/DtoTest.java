package com.eventledger.accountservice.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    // === TransactionRequest Tests ===

    @Test
    void testTransactionRequestDefaultConstructor() {
        TransactionRequest req = new TransactionRequest();
        assertNull(req.getEventId());
        assertNull(req.getAccountId());
        assertNull(req.getType());
        assertNull(req.getAmount());
        assertNull(req.getCurrency());
        assertNull(req.getEventTimestamp());
        assertNull(req.getMetadata());
    }

    @Test
    void testTransactionRequestSettersAndGetters() {
        TransactionRequest req = new TransactionRequest();
        Instant now = Instant.now();

        req.setEventId("evt-1");
        req.setAccountId("acct-1");
        req.setType("CREDIT");
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        req.setEventTimestamp(now);
        req.setMetadata("{\"key\":\"value\"}");

        assertEquals("evt-1", req.getEventId());
        assertEquals("acct-1", req.getAccountId());
        assertEquals("CREDIT", req.getType());
        assertEquals(new BigDecimal("100.00"), req.getAmount());
        assertEquals("USD", req.getCurrency());
        assertEquals(now, req.getEventTimestamp());
        assertEquals("{\"key\":\"value\"}", req.getMetadata());
    }

    @Test
    void testTransactionRequestAllArgsConstructor() {
        Instant now = Instant.now();
        TransactionRequest req = new TransactionRequest(
                "evt-1", "acct-1", "DEBIT",
                new BigDecimal("50.00"), "EUR", now, "{}"
        );

        assertEquals("evt-1", req.getEventId());
        assertEquals("acct-1", req.getAccountId());
        assertEquals("DEBIT", req.getType());
        assertEquals(new BigDecimal("50.00"), req.getAmount());
        assertEquals("EUR", req.getCurrency());
        assertEquals(now, req.getEventTimestamp());
        assertEquals("{}", req.getMetadata());
    }

    // === TransactionResponse Tests ===

    @Test
    void testTransactionResponseDefaultConstructor() {
        TransactionResponse res = new TransactionResponse();
        assertNull(res.getEventId());
        assertNull(res.getAccountId());
        assertNull(res.getType());
        assertNull(res.getAmount());
        assertNull(res.getCurrency());
        assertNull(res.getEventTimestamp());
        assertNull(res.getCreatedAt());
        assertNull(res.getMetadata());
    }

    @Test
    void testTransactionResponseSettersAndGetters() {
        TransactionResponse res = new TransactionResponse();
        Instant now = Instant.now();
        Instant created = Instant.now();

        res.setEventId("evt-2");
        res.setAccountId("acct-2");
        res.setType("CREDIT");
        res.setAmount(new BigDecimal("200.00"));
        res.setCurrency("GBP");
        res.setEventTimestamp(now);
        res.setCreatedAt(created);
        res.setMetadata("{\"m\":\"v\"}");

        assertEquals("evt-2", res.getEventId());
        assertEquals("acct-2", res.getAccountId());
        assertEquals("CREDIT", res.getType());
        assertEquals(new BigDecimal("200.00"), res.getAmount());
        assertEquals("GBP", res.getCurrency());
        assertEquals(now, res.getEventTimestamp());
        assertEquals(created, res.getCreatedAt());
        assertEquals("{\"m\":\"v\"}", res.getMetadata());
    }

    @Test
    void testTransactionResponseAllArgsConstructor() {
        Instant now = Instant.now();
        Instant created = Instant.now();
        TransactionResponse res = new TransactionResponse(
                "evt-3", "acct-3", "DEBIT",
                new BigDecimal("300.00"), "USD", now, created, "{}"
        );

        assertEquals("evt-3", res.getEventId());
        assertEquals("acct-3", res.getAccountId());
        assertEquals("DEBIT", res.getType());
        assertEquals(new BigDecimal("300.00"), res.getAmount());
        assertEquals("USD", res.getCurrency());
        assertEquals(now, res.getEventTimestamp());
        assertEquals(created, res.getCreatedAt());
        assertEquals("{}", res.getMetadata());
    }

    // === BalanceResponse Tests ===

    @Test
    void testBalanceResponseDefaultConstructor() {
        BalanceResponse res = new BalanceResponse();
        assertNull(res.getAccountId());
        assertNull(res.getBalance());
        assertNull(res.getCurrency());
    }

    @Test
    void testBalanceResponseSettersAndGetters() {
        BalanceResponse res = new BalanceResponse();
        res.setAccountId("acct-bal");
        res.setBalance(new BigDecimal("999.99"));
        res.setCurrency("USD");

        assertEquals("acct-bal", res.getAccountId());
        assertEquals(new BigDecimal("999.99"), res.getBalance());
        assertEquals("USD", res.getCurrency());
    }

    @Test
    void testBalanceResponseAllArgsConstructor() {
        BalanceResponse res = new BalanceResponse("acct-1", new BigDecimal("500.00"), "EUR");

        assertEquals("acct-1", res.getAccountId());
        assertEquals(new BigDecimal("500.00"), res.getBalance());
        assertEquals("EUR", res.getCurrency());
    }

    // === AccountResponse Tests ===

    @Test
    void testAccountResponseDefaultConstructor() {
        AccountResponse res = new AccountResponse();
        assertNull(res.getAccountId());
        assertNull(res.getBalance());
        assertNull(res.getCurrency());
        assertNull(res.getRecentTransactions());
    }

    @Test
    void testAccountResponseSettersAndGetters() {
        AccountResponse res = new AccountResponse();
        List<TransactionResponse> txns = new ArrayList<>();
        txns.add(new TransactionResponse("evt-1", "acct-1", "CREDIT",
                new BigDecimal("100"), "USD", Instant.now(), Instant.now(), "{}"));

        res.setAccountId("acct-acc");
        res.setBalance(new BigDecimal("100.00"));
        res.setCurrency("USD");
        res.setRecentTransactions(txns);

        assertEquals("acct-acc", res.getAccountId());
        assertEquals(new BigDecimal("100.00"), res.getBalance());
        assertEquals("USD", res.getCurrency());
        assertEquals(1, res.getRecentTransactions().size());
    }

    @Test
    void testAccountResponseAllArgsConstructor() {
        List<TransactionResponse> txns = new ArrayList<>();
        AccountResponse res = new AccountResponse("acct-1", new BigDecimal("750"), "USD", txns);

        assertEquals("acct-1", res.getAccountId());
        assertEquals(new BigDecimal("750"), res.getBalance());
        assertEquals("USD", res.getCurrency());
        assertNotNull(res.getRecentTransactions());
        assertTrue(res.getRecentTransactions().isEmpty());
    }
}

