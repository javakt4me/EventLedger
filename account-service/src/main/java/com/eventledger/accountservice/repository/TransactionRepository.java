package com.eventledger.accountservice.repository;

import com.eventledger.accountservice.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByEventId(String eventId);

    List<Transaction> findByAccountIdOrderByEventTimestamp(String accountId);
}

