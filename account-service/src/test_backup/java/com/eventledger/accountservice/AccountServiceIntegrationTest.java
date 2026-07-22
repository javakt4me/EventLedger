package com.eventledger.accountservice;

import com.eventledger.accountservice.dto.TransactionRequest;
import com.eventledger.accountservice.domain.TransactionType;
import com.eventledger.accountservice.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    private TransactionRequest validTransactionRequest;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        validTransactionRequest = new TransactionRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                Instant.now(),
                "{\"source\": \"test\"}"
        );
    }

    @Test
    void testCreateTransactionSuccess() throws Exception {
        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTransactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", is("evt-001")))
                .andExpect(jsonPath("$.accountId", is("acct-123")))
                .andExpect(jsonPath("$.type", is("CREDIT")))
                .andExpect(jsonPath("$.amount", is(150.00)))
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void testCreateTransactionIdempotency() throws Exception {
        // First request
        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTransactionRequest)))
                .andExpect(status().isCreated());

        // Second request with same event ID
        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTransactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", is("evt-001")));
    }

    @Test
    void testGetBalanceCreditOnly() throws Exception {
        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTransactionRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/acct-123/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is("acct-123")))
                .andExpect(jsonPath("$.balance", is(150.00)))
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void testGetBalanceMultipleTransactions() throws Exception {
        // First transaction: CREDIT 150
        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTransactionRequest)))
                .andExpect(status().isCreated());

        // Second transaction: DEBIT 50
        TransactionRequest debitRequest = new TransactionRequest(
                "evt-002",
                "acct-123",
                "DEBIT",
                new BigDecimal("50.00"),
                "USD",
                Instant.now().plusSeconds(1),
                null
        );

        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isCreated());

        // Check balance = 150 - 50 = 100
        mockMvc.perform(get("/accounts/acct-123/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is("acct-123")))
                .andExpect(jsonPath("$.balance", is(100.00)))
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void testGetAccountDetails() throws Exception {
        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTransactionRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/acct-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is("acct-123")))
                .andExpect(jsonPath("$.balance", is(150.00)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.recentTransactions", hasSize(1)))
                .andExpect(jsonPath("$.recentTransactions[0].eventId", is("evt-001")));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/accounts/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void testGetBalanceEmptyAccount() throws Exception {
        mockMvc.perform(get("/accounts/acct-new/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is("acct-new")))
                .andExpect(jsonPath("$.balance").exists())
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void testOutOfOrderTransactions() throws Exception {
        Instant baseTime = Instant.now();

        // Create transaction with later timestamp first
        TransactionRequest laterTransaction = new TransactionRequest(
                "evt-002",
                "acct-123",
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                baseTime.plusSeconds(10),
                null
        );

        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(laterTransaction)))
                .andExpect(status().isCreated());

        // Create transaction with earlier timestamp
        TransactionRequest earlierTransaction = new TransactionRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("50.00"),
                "USD",
                baseTime,
                null
        );

        mockMvc.perform(post("/accounts/acct-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(earlierTransaction)))
                .andExpect(status().isCreated());

        // Get account - transactions should be ordered by eventTimestamp
        mockMvc.perform(get("/accounts/acct-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentTransactions", hasSize(2)))
                .andExpect(jsonPath("$.recentTransactions[0].eventId", is("evt-001")))  // Earlier
                .andExpect(jsonPath("$.recentTransactions[1].eventId", is("evt-002")));  // Later
    }
}

