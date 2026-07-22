package com.eventledger.accountservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class AccountResponse {
    private String accountId;
    private BigDecimal balance;
    private String currency;
    private List<TransactionResponse> recentTransactions;

    public AccountResponse() {
    }

    public AccountResponse(String accountId, BigDecimal balance, String currency,
                         List<TransactionResponse> recentTransactions) {
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
        this.recentTransactions = recentTransactions;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<TransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}

