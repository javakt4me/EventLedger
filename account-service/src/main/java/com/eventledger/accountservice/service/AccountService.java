package com.eventledger.accountservice.service;

import com.eventledger.accountservice.domain.Transaction;
import com.eventledger.accountservice.domain.TransactionType;
import com.eventledger.accountservice.dto.AccountResponse;
import com.eventledger.accountservice.dto.BalanceResponse;
import com.eventledger.accountservice.dto.TransactionRequest;
import com.eventledger.accountservice.dto.TransactionResponse;
import com.eventledger.accountservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private final TransactionRepository transactionRepository;

    public AccountService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse createTransaction(TransactionRequest request) {
        // Check if transaction already exists (idempotency)
        Optional<Transaction> existing = transactionRepository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            return convertToResponse(existing.get());
        }

        Transaction transaction = new Transaction(
                request.getEventId(),
                request.getAccountId(),
                TransactionType.valueOf(request.getType()),
                request.getAmount(),
                request.getCurrency(),
                request.getEventTimestamp(),
                request.getMetadata()
        );

        Transaction saved = transactionRepository.save(transaction);
        return convertToResponse(saved);
    }

    public BalanceResponse getBalance(String accountId) {
        List<Transaction> transactions = transactionRepository
                .findByAccountIdOrderByEventTimestamp(accountId);

        BigDecimal balance = calculateBalance(transactions);

        String currency = "USD";
        if (!transactions.isEmpty()) {
            currency = transactions.get(0).getCurrency();
        }

        return new BalanceResponse(accountId, balance, currency);
    }

    public AccountResponse getAccount(String accountId) {
        List<Transaction> transactions = transactionRepository
                .findByAccountIdOrderByEventTimestamp(accountId);

        BigDecimal balance = calculateBalance(transactions);

        String currency = "USD";
        if (!transactions.isEmpty()) {
            currency = transactions.get(0).getCurrency();
        }

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new AccountResponse(accountId, balance, currency, transactionResponses);
    }

    private BigDecimal calculateBalance(List<Transaction> transactions) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.CREDIT) {
                balance = balance.add(transaction.getAmount());
            } else {
                balance = balance.subtract(transaction.getAmount());
            }
        }
        return balance;
    }

    private TransactionResponse convertToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getEventId(),
                transaction.getAccountId(),
                transaction.getType().toString(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getEventTimestamp(),
                transaction.getCreatedAt(),
                transaction.getMetadata()
        );
    }
}

