package com.eventledger.accountservice.controller;

import com.eventledger.accountservice.dto.AccountResponse;
import com.eventledger.accountservice.dto.BalanceResponse;
import com.eventledger.accountservice.dto.TransactionRequest;
import com.eventledger.accountservice.dto.TransactionResponse;
import com.eventledger.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

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
        
        transactionRequest = new TransactionRequest(
                "evt-001",
                "acct-123",
                "CREDIT",
                new BigDecimal("150.00"),
                "USD",
                now,
                "{\"source\": \"test\"}"
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

    @Test
    void testHealth() {
        ResponseEntity<String> response = accountController.health();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
    }
}

