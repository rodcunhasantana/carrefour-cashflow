package com.carrefourbank.transaction.infrastructure.adapter.persistence;

import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.transaction.domain.model.Transaction;
import com.carrefourbank.transaction.domain.model.TransactionStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@Tag("persistence")
@Tag("FR-01")
@Tag("FR-03")
@Tag("FR-04")
@Tag("NFR-06")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcTransactionRepositoryTest {

    @Autowired
    private JdbcTransactionRepository repository;

    private static final LocalDate DATE = LocalDate.of(2026, 4, 14);

    @Test
    void save_and_findById_round_trips() {
        Transaction tx = Transaction.create(
                TransactionType.CREDIT, Money.ofBRL(new BigDecimal("100.00")), DATE, "Round trip");
        repository.save(tx);

        Optional<Transaction> found = repository.findById(tx.id());
        assertTrue(found.isPresent());
        assertEquals(tx.id(), found.get().id());
        assertEquals(TransactionType.CREDIT, found.get().type());
        assertEquals(0, new BigDecimal("100.00").compareTo(found.get().amount().amount()));
    }

    @Test
    void save_updates_status_when_record_exists() {
        Transaction tx = Transaction.create(
                TransactionType.DEBIT, Money.ofBRL(new BigDecimal("-30.00")), DATE, "Update test");
        repository.save(tx);
        repository.save(tx.withStatus(TransactionStatus.REVERSED));

        Transaction found = repository.findById(tx.id()).orElseThrow();
        assertEquals(TransactionStatus.REVERSED, found.status());
    }

    @Test
    void findById_returns_empty_for_unknown_id() {
        Optional<Transaction> result = repository.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void existsReversalFor_returns_false_when_no_reversal() {
        Transaction tx = Transaction.create(
                TransactionType.CREDIT, Money.ofBRL(new BigDecimal("50.00")), DATE, "No reversal yet");
        repository.save(tx);
        assertFalse(repository.existsReversalFor(tx.id()));
    }

    @Test
    void existsReversalFor_returns_true_after_reversal_saved() {
        Transaction original = Transaction.create(
                TransactionType.CREDIT, Money.ofBRL(new BigDecimal("200.00")), DATE, "To be reversed");
        repository.save(original);
        Transaction reversal = Transaction.createReversal(original, "Correction");
        repository.save(reversal);

        assertTrue(repository.existsReversalFor(original.id()));
    }

    @Test
    void findAll_without_filters_returns_all() {
        repository.save(Transaction.create(TransactionType.CREDIT, Money.ofBRL(new BigDecimal("10.00")), DATE, "A"));
        repository.save(Transaction.create(TransactionType.DEBIT, Money.ofBRL(new BigDecimal("-5.00")), DATE, "B"));

        List<Transaction> results = repository.findAll(null, null, null, 0, 20);
        assertTrue(results.size() >= 2);
    }

    @Test
    void findAll_filters_by_type() {
        repository.save(Transaction.create(TransactionType.CREDIT, Money.ofBRL(new BigDecimal("10.00")), DATE, "Credit only"));
        repository.save(Transaction.create(TransactionType.DEBIT, Money.ofBRL(new BigDecimal("-5.00")), DATE, "Debit only"));

        List<Transaction> credits = repository.findAll(null, null, TransactionType.CREDIT, 0, 20);
        credits.forEach(t -> assertEquals(TransactionType.CREDIT, t.type()));
    }

    @Test
    void count_returns_total_matching_records() {
        repository.save(Transaction.create(TransactionType.CREDIT, Money.ofBRL(new BigDecimal("10.00")), DATE, "Count test 1"));
        repository.save(Transaction.create(TransactionType.CREDIT, Money.ofBRL(new BigDecimal("20.00")), DATE, "Count test 2"));

        long count = repository.count(DATE, DATE, TransactionType.CREDIT);
        assertTrue(count >= 2);
    }
}
