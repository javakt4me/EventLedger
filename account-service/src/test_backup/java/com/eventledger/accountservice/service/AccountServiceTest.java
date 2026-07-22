package com.eventledger.accountservice.service;

import com.eventledger.accountservice.domain.Transaction;
import com.eventledger.accountservice.domain.TransactionType;
import com.eventledger.accountservice.dto.AccountResponse;
import com.eventledger.accountservice.dto.BalanceResponse;
import com.eventledger.accountservice.dto.TransactionRequest;
import com.eventledger.accountservice.dto.TransactionResponse;
import com.eventledger.accountservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private TransactionRequest transactionRequest;
    private Transaction transaction;
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

        transaction = new Transaction(
                "evt-001",
                "acct-123",
                TransactionType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                now,
                "{\"source\": \"test\"}"
        );
        transaction.setId(1L);
        transaction.setCreatedAt(now);
    }

    @Test
    void testCreateTransactionNewEvent() {
        when(transactionRepository.findByEventId("evt-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponse response = accountService.createTransaction(transactionRequest);

        assertNotNull(response);
        assertEquals("evt-001", response.getEventId());
        assertEquals("acct-123", response.getAccountId());
        assertEquals("CREDIT", response.getType());
        assertEquals(new BigDecimal("150.00"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testCreateTransactionIdempotency() {
        when(transactionRepository.findByEventId("evt-001")).thenReturn(Optional.of(transaction));

        TransactionResponse response = accountService.createTransaction(transactionRequest);

        assertNotNull(response);
        assertEquals("evt-001", response.getEventId());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testGetBalanceCreditOnly() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        when(transactionRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(transactions);

        BalanceResponse response = accountService.getBalance("acct-123");

        assertNotNull(response);
        assertEquals("acct-123", response.getAccountId());
        assertEquals(new BigDecimal("150.00"), response.getBalance());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void testGetBalanceDebitOnly() {
        Transaction debitTransaction = new Transaction(
                "evt-002",
                "acct-123",
                TransactionType.DEBIT,
                new BigDecimal("50.00"),
                "USD",
                now,
                null
        );
        debitTransaction.setId(2L);
        debitTransaction.setCreatedAt(now);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(debitTransaction);

        when(transactionRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(transactions);

        BalanceResponse response = accountService.getBalance("acct-123");

        assertNotNull(response);
        assertEquals("acct-123", response.getAccountId());
        assertEquals(new BigDecimal("-50.00"), response.getBalance());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void testGetBalanceMixed() {
        Transaction debitTransaction = new Transaction(
                "evt-002",
                "acct-123",
                TransactionType.DEBIT,
                new BigDecimal("50.00"),
                "USD",
                now,
                null
        );
        debitTransaction.setId(2L);
        debitTransaction.setCreatedAt(now);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);
        transactions.add(debitTransaction);

        when(transactionRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(transactions);

        BalanceResponse response = accountService.getBalance("acct-123");

        assertNotNull(response);
        assertEquals("acct-123", response.getAccountId());
        assertEquals(new BigDecimal("100.00"), response.getBalance());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void testGetBalanceEmptyAccount() {
        List<Transaction> transactions = new ArrayList<>();

        when(transactionRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(transactions);

        BalanceResponse response = accountService.getBalance("acct-123");

        assertNotNull(response);
        assertEquals("acct-123", response.getAccountId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void testGetAccount() {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        when(transactionRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(transactions);

        AccountResponse response = accountService.getAccount("acct-123");

        assertNotNull(response);
        assertEquals("acct-123", response.getAccountId());
        assertEquals(new BigDecimal("150.00"), response.getBalance());
        assertEquals("USD", response.getCurrency());
        assertEquals(1, response.getRecentTransactions().size());
        assertEquals("evt-001", response.getRecentTransactions().get(0).getEventId());
    }

    @Test
    void testGetAccountEmptyTransactions() {
        List<Transaction> transactions = new ArrayList<>();

        when(transactionRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(transactions);

        AccountResponse response = accountService.getAccount("acct-123");

        assertNotNull(response);
        assertEquals("acct-123", response.getAccountId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        assertEquals("USD", response.getCurrency());
        assertEquals(0, response.getRecentTransactions().size());
    }

    @Test
    void testGetAccountMultipleTransactions() {
        Transaction debitTransaction = new Transaction(
                "evt-002",
                "acct-123",
                TransactionType.DEBIT,
                new BigDecimal("50.00"),
                "USD",
                Instant.now().plusSeconds(1),
                null
        );
        debitTransaction.setId(2L);
        debitTransaction.setCreatedAt(Instant.now().plusSeconds(1));

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);
        transactions.add(debitTransaction);

        when(transactionRepository.findByAccountIdOrderByEventTimestamp("acct-123"))
                .thenReturn(transactions);

        AccountResponse response = accountService.getAccount("acct-123");

        assertNotNull(response);
        assertEquals("acct-123", response.getAccountId());
        assertEquals(new BigDecimal("100.00"), response.getBalance());
        assertEquals("USD", response.getCurrency());
        assertEquals(2, response.getRecentTransactions().size());
    }
}

