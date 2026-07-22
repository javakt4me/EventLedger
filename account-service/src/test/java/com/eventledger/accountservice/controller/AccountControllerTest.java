package com.eventledger.accountservice.controller;

import com.eventledger.accountservice.dto.AccountResponse;
import com.eventledger.accountservice.dto.BalanceResponse;
import com.eventledger.accountservice.dto.TransactionRequest;
import com.eventledger.accountservice.dto.TransactionResponse;
import com.eventledger.accountservice.metrics.AccountServiceMetrics;
import com.eventledger.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountServiceMetrics metrics;

    @InjectMocks
    private AccountController accountController;

    private TransactionRequest transactionRequest;
    private TransactionResponse transactionResponse;
    private BalanceResponse balanceResponse;
    private AccountResponse accountResponse;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        Map<String, Object> metadata = Collections.singletonMap("source", "test");
        
        transactionRequest = new TransactionRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                metadata
        );

        transactionResponse = new TransactionResponse(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                now,
                "{\"source\": \"test\"}"
        );

        balanceResponse = new BalanceResponse("acct-123", new BigDecimal("150.00"), "USD");

        accountResponse = new AccountResponse(
                "acct-123",
                new BigDecimal("150.00"),
                "USD",
                Arrays.asList(transactionResponse)
        );

        // Setup metrics mocks
        doNothing().when(metrics).incrementActiveTransactions();
        doNothing().when(metrics).decrementActiveTransactions();
        doNothing().when(metrics).recordTransactionCreated();
        doNothing().when(metrics).recordBalanceCheck();
        doNothing().when(metrics).recordError();
        when(metrics.transactionTimer()).thenReturn(io.micrometer.core.instrument.Timer.builder("test").register(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        when(metrics.balanceCheckTimer()).thenReturn(io.micrometer.core.instrument.Timer.builder("test").register(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    }

    @Test
    void testCreateTransaction() {
        when(accountService.createTransaction(any(TransactionRequest.class)))
                .thenReturn(transactionResponse);

        ResponseEntity<TransactionResponse> response = accountController.createTransaction(
                "acct-123",
                transactionRequest
        );

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(transactionResponse, response.getBody());
        verify(accountService, times(1)).createTransaction(any(TransactionRequest.class));
        verify(metrics, times(1)).incrementActiveTransactions();
        verify(metrics, times(1)).recordTransactionCreated();
    }

    @Test
    void testGetBalance() {
        when(accountService.getBalance("acct-123"))
                .thenReturn(balanceResponse);

        ResponseEntity<BalanceResponse> response = accountController.getBalance("acct-123");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(balanceResponse, response.getBody());
        assertEquals("acct-123", response.getBody().getAccountId());
        assertEquals(new BigDecimal("150.00"), response.getBody().getBalance());
        verify(accountService, times(1)).getBalance("acct-123");
        verify(metrics, times(1)).recordBalanceCheck();
    }



    @Test
    void testGetAccount() {
        when(accountService.getAccount("acct-123"))
                .thenReturn(accountResponse);

        ResponseEntity<AccountResponse> response = accountController.getAccount("acct-123");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(accountResponse, response.getBody());
        assertEquals("acct-123", response.getBody().getAccountId());
        assertEquals(1, response.getBody().getRecentTransactions().size());
        verify(accountService, times(1)).getAccount("acct-123");
    }


}
