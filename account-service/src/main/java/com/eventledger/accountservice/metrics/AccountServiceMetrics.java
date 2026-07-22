package com.eventledger.accountservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AccountServiceMetrics {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceMetrics.class);

    private final Counter transactionCounter;
    private final Counter balanceCheckCounter;
    private final Counter errorCounter;
    private final Timer transactionTimerObj;
    private final Timer balanceCheckTimerObj;
    private final AtomicInteger activeTransactions;

    public AccountServiceMetrics(MeterRegistry meterRegistry) {
        this.transactionCounter = Counter.builder("account.transactions.created")
                .description("Total number of transactions created")
                .register(meterRegistry);

        this.balanceCheckCounter = Counter.builder("account.balance.checks")
                .description("Total number of balance checks")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("account.errors.total")
                .description("Total number of errors")
                .register(meterRegistry);

        this.transactionTimerObj = Timer.builder("account.transaction.duration")
                .description("Time taken to create a transaction")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.balanceCheckTimerObj = Timer.builder("account.balance.check.duration")
                .description("Time taken to check balance")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.activeTransactions = meterRegistry.gauge("account.active.transactions",
                new AtomicInteger(0),
                AtomicInteger::get);

        logger.info("Account Service Metrics initialized");
    }

    public void recordTransactionCreated() {
        transactionCounter.increment();
        logger.debug("Recorded transaction created metric");
    }

    public void recordBalanceCheck() {
        balanceCheckCounter.increment();
        logger.debug("Recorded balance check metric");
    }

    public void recordError() {
        errorCounter.increment();
        logger.debug("Recorded error metric");
    }

    public Timer.Sample startTransactionTimer() {
        return Timer.start();
    }

    public Timer.Sample startBalanceCheckTimer() {
        return Timer.start();
    }

    public Timer transactionTimer() {
        return transactionTimerObj;
    }

    public Timer balanceCheckTimer() {
        return balanceCheckTimerObj;
    }

    public void incrementActiveTransactions() {
        activeTransactions.incrementAndGet();
    }

    public void decrementActiveTransactions() {
        activeTransactions.decrementAndGet();
    }
}

