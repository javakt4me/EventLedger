package com.eventledger.accountservice.controller;

import com.eventledger.accountservice.dto.AccountResponse;
import com.eventledger.accountservice.dto.BalanceResponse;
import com.eventledger.accountservice.dto.TransactionRequest;
import com.eventledger.accountservice.dto.TransactionResponse;
import com.eventledger.accountservice.metrics.AccountServiceMetrics;
import com.eventledger.accountservice.service.AccountService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;
    private final AccountServiceMetrics metrics;

    public AccountController(AccountService accountService, AccountServiceMetrics metrics) {
        this.accountService = accountService;
        this.metrics = metrics;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable String accountId,
            @RequestBody TransactionRequest request) {
        metrics.incrementActiveTransactions();
        Timer.Sample sample = Timer.start();
        
        try {
            logger.debug("Creating transaction for account: {}", accountId);
            request.setAccountId(accountId);
            TransactionResponse response = accountService.createTransaction(request);
            metrics.recordTransactionCreated();
            logger.info("Transaction created successfully for account: {}", accountId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            metrics.recordError();
            logger.error("Error creating transaction for account: {}", accountId, e);
            throw e;
        } finally {
            sample.stop(metrics.transactionTimer());
            metrics.decrementActiveTransactions();
        }
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.debug("Fetching balance for account: {}", accountId);
            BalanceResponse response = accountService.getBalance(accountId);
            metrics.recordBalanceCheck();
            logger.info("Balance retrieved for account: {}", accountId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            metrics.recordError();
            logger.error("Error fetching balance for account: {}", accountId, e);
            throw e;
        } finally {
            sample.stop(metrics.balanceCheckTimer());
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        logger.debug("Fetching account details for account: {}", accountId);
        try {
            AccountResponse response = accountService.getAccount(accountId);
            logger.info("Account details retrieved for account: {}", accountId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            metrics.recordError();
            logger.error("Error fetching account: {}", accountId, e);
            throw e;
        }
    }
}

