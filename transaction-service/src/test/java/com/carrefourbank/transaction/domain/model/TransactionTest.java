package com.carrefourbank.transaction.domain.model;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.common.exception.ValidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@Tag("domain")
@Tag("FR-01")
@Tag("FR-02")
@Tag("FR-03")
@Tag("FR-04")
@Tag("NFR-07")
class TransactionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 14);

    @Test
    void create_credit_with_positive_amount_succeeds() {
        Money amount = Money.ofBRL(new BigDecimal("100.00"));
        Transaction tx = Transaction.create(TransactionType.CREDIT, amount, TODAY, "Test credit");
        assertNotNull(tx.id());
        assertEquals(TransactionType.CREDIT, tx.type());
        assertEquals(amount, tx.amount());
        assertEquals(TODAY, tx.date());
        assertEquals("Test credit", tx.description());
        assertEquals(TransactionStatus.PENDING, tx.status());
        assertFalse(tx.isReversal());
        assertNull(tx.originalTransactionId());
    }

    @Test
    void create_debit_with_negative_amount_succeeds() {
        Money amount = Money.ofBRL(new BigDecimal("-50.00"));
        Transaction tx = Transaction.create(TransactionType.DEBIT, amount, TODAY, "Test debit");
        assertEquals(TransactionType.DEBIT, tx.type());
        assertEquals(amount, tx.amount());
    }

    @Test
    void create_credit_with_negative_amount_throws_validation_exception() {
        Money negativeAmount = Money.ofBRL(new BigDecimal("-10.00"));
        assertThrows(ValidationException.class,
                () -> Transaction.create(TransactionType.CREDIT, negativeAmount, TODAY, "Bad credit"));
    }

    @Test
    void create_debit_with_positive_amount_throws_validation_exception() {
        Money positiveAmount = Money.ofBRL(new BigDecimal("10.00"));
        assertThrows(ValidationException.class,
                () -> Transaction.create(TransactionType.DEBIT, positiveAmount, TODAY, "Bad debit"));
    }

    @Test
    void create_with_blank_description_throws_validation_exception() {
        Money amount = Money.ofBRL(new BigDecimal("100.00"));
        assertThrows(ValidationException.class,
                () -> Transaction.create(TransactionType.CREDIT, amount, TODAY, "  "));
    }

    @Test
    void create_with_null_type_throws_validation_exception() {
        Money amount = Money.ofBRL(new BigDecimal("100.00"));
        assertThrows(ValidationException.class,
                () -> Transaction.create(null, amount, TODAY, "No type"));
    }

    @Test
    void createReversal_inverts_amount_and_sets_reversal_fields() {
        Money amount = Money.ofBRL(new BigDecimal("200.00"));
        Transaction original = Transaction.create(TransactionType.CREDIT, amount, TODAY, "Original");
        Transaction reversal = Transaction.createReversal(original, "Wrong entry");

        // Estorno de CREDIT deve ser DEBIT com valor negativo
        assertEquals(TransactionType.DEBIT, reversal.type());
        assertTrue(reversal.amount().isNegative());
        assertEquals(new BigDecimal("-200.00"), reversal.amount().amount());
        assertTrue(reversal.isReversal());
        assertEquals(original.id(), reversal.originalTransactionId());
        assertEquals(TransactionStatus.COMPLETED, reversal.status());
        assertTrue(reversal.description().contains("Reversal:"));
        assertTrue(reversal.description().contains("Wrong entry"));
    }

    @Test
    void createReversal_with_blank_reason_throws_validation_exception() {
        Money amount = Money.ofBRL(new BigDecimal("100.00"));
        Transaction original = Transaction.create(TransactionType.CREDIT, amount, TODAY, "Original");
        assertThrows(ValidationException.class,
                () -> Transaction.createReversal(original, "  "));
    }

    @Test
    void withStatus_returns_new_instance_with_updated_status() {
        Money amount = Money.ofBRL(new BigDecimal("100.00"));
        Transaction tx = Transaction.create(TransactionType.CREDIT, amount, TODAY, "Test");
        Transaction updated = tx.withStatus(TransactionStatus.REVERSED);

        assertEquals(TransactionStatus.PENDING, tx.status());
        assertEquals(TransactionStatus.REVERSED, updated.status());
        assertEquals(tx.id(), updated.id());
    }
}
