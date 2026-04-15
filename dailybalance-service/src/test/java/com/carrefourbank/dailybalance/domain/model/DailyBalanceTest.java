package com.carrefourbank.dailybalance.domain.model;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.exception.ValidationException;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyClosedException;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyOpenException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DailyBalanceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 14);
    private static final Money ZERO = Money.zero(Currency.BRL);
    private static final Money OPENING = Money.of(new BigDecimal("1000.00"), Currency.BRL);

    @Test
    void create_initializesWithZeroCreditsDebitsAndOpenStatus() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);

        assertEquals(DATE, balance.date());
        assertEquals(OPENING.amount(), balance.openingBalance().amount());
        assertEquals(BigDecimal.ZERO, balance.totalCredits().amount());
        assertEquals(BigDecimal.ZERO, balance.totalDebits().amount());
        assertEquals(OPENING.amount(), balance.closingBalance().amount());
        assertEquals(BalanceStatus.OPEN, balance.status());
        assertNull(balance.closedAt());
        assertNotNull(balance.createdAt());
        assertNotNull(balance.updatedAt());
        assertNotNull(balance.id());
    }

    @Test
    void create_throwsValidationException_whenDateIsNull() {
        assertThrows(ValidationException.class, () -> DailyBalance.create(null, OPENING));
    }

    @Test
    void create_throwsValidationException_whenOpeningBalanceIsNull() {
        assertThrows(ValidationException.class, () -> DailyBalance.create(DATE, null));
    }

    @Test
    void withAddedCredit_increasesTotalCreditsAndRecalculatesClosing() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        Money credit = Money.of(new BigDecimal("200.00"), Currency.BRL);

        DailyBalance updated = balance.withAddedCredit(credit);

        assertEquals(new BigDecimal("200.00"), updated.totalCredits().amount());
        assertEquals(BigDecimal.ZERO, updated.totalDebits().amount());
        assertEquals(new BigDecimal("1200.00"), updated.closingBalance().amount());
    }

    @Test
    void withAddedCredit_throwsValidationException_whenAmountIsNegative() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        Money negative = Money.of(new BigDecimal("-100.00"), Currency.BRL);

        assertThrows(ValidationException.class, () -> balance.withAddedCredit(negative));
    }

    @Test
    void withAddedDebit_increasesTotalDebitsAndRecalculatesClosing() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        Money debit = Money.of(new BigDecimal("-300.00"), Currency.BRL);

        DailyBalance updated = balance.withAddedDebit(debit);

        assertEquals(BigDecimal.ZERO, updated.totalCredits().amount());
        assertEquals(new BigDecimal("-300.00"), updated.totalDebits().amount());
        assertEquals(new BigDecimal("700.00"), updated.closingBalance().amount());
    }

    @Test
    void withAddedDebit_throwsValidationException_whenAmountIsPositive() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        Money positive = Money.of(new BigDecimal("100.00"), Currency.BRL);

        assertThrows(ValidationException.class, () -> balance.withAddedDebit(positive));
    }

    @Test
    void closingBalance_formula_openingPlusCreditsMinusDebits() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        Money credit = Money.of(new BigDecimal("500.00"), Currency.BRL);
        Money debit = Money.of(new BigDecimal("-200.00"), Currency.BRL);

        DailyBalance updated = balance.withAddedCredit(credit).withAddedDebit(debit);

        // 1000 + 500 + (-200) = 1300
        assertEquals(new BigDecimal("1300.00"), updated.closingBalance().amount());
    }

    @Test
    void close_setsStatusClosedAndClosedAt() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);

        DailyBalance closed = balance.close();

        assertEquals(BalanceStatus.CLOSED, closed.status());
        assertNotNull(closed.closedAt());
        assertEquals(BalanceStatus.OPEN, balance.status()); // original unchanged
    }

    @Test
    void close_throwsBalanceAlreadyClosedException_whenAlreadyClosed() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING).close();

        assertThrows(BalanceAlreadyClosedException.class, balance::close);
    }

    @Test
    void withAddedCredit_throwsBalanceAlreadyClosedException_whenClosed() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING).close();
        Money credit = Money.of(new BigDecimal("100.00"), Currency.BRL);

        assertThrows(BalanceAlreadyClosedException.class, () -> balance.withAddedCredit(credit));
    }

    @Test
    void reopen_setsStatusOpenAndClearsClosedAt() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING).close();

        DailyBalance reopened = balance.reopen();

        assertEquals(BalanceStatus.OPEN, reopened.status());
        assertNull(reopened.closedAt());
    }

    @Test
    void reopen_throwsBalanceAlreadyOpenException_whenAlreadyOpen() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);

        assertThrows(BalanceAlreadyOpenException.class, balance::reopen);
    }

    @Test
    void recalculate_recomputesClosingBalance() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING)
                .withAddedCredit(Money.of(new BigDecimal("400.00"), Currency.BRL))
                .withAddedDebit(Money.of(new BigDecimal("-100.00"), Currency.BRL));

        DailyBalance recalculated = balance.recalculate();

        // 1000 + 400 + (-100) = 1300
        assertEquals(new BigDecimal("1300.00"), recalculated.closingBalance().amount());
        assertEquals(balance.id(), recalculated.id());
    }
}
