package com.carrefourbank.transaction.domain.port;

import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.transaction.domain.model.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    boolean existsReversalFor(UUID originalId);

    List<Transaction> findAll(LocalDate startDate, LocalDate endDate, TransactionType type, int page, int size);

    long count(LocalDate startDate, LocalDate endDate, TransactionType type);
}
