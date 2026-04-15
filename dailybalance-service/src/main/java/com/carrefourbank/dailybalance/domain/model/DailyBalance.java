package com.carrefourbank.dailybalance.domain.model;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.exception.ValidationException;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyClosedException;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyOpenException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class DailyBalance {

    private final UUID id;
    private final LocalDate date;
    private final Money openingBalance;
    private final Money totalCredits;
    private final Money totalDebits;
    private final Money closingBalance;
    private final BalanceStatus status;
    private final LocalDateTime closedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private DailyBalance(
            UUID id,
            LocalDate date,
            Money openingBalance,
            Money totalCredits,
            Money totalDebits,
            Money closingBalance,
            BalanceStatus status,
            LocalDateTime closedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.date = date;
        this.openingBalance = openingBalance;
        this.totalCredits = totalCredits;
        this.totalDebits = totalDebits;
        this.closingBalance = closingBalance;
        this.status = status;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DailyBalance create(LocalDate date, Money openingBalance) {
        if (date == null) {
            throw new ValidationException("Date is required");
        }
        if (openingBalance == null) {
            throw new ValidationException("Opening balance is required");
        }
        Money zero = Money.zero(openingBalance.currency());
        LocalDateTime now = LocalDateTime.now();
        return new DailyBalance(
                UUID.randomUUID(),
                date,
                openingBalance,
                zero,
                zero,
                openingBalance,
                BalanceStatus.OPEN,
                null,
                now,
                now);
    }

    public static DailyBalance restore(
            UUID id,
            LocalDate date,
            Money openingBalance,
            Money totalCredits,
            Money totalDebits,
            Money closingBalance,
            BalanceStatus status,
            LocalDateTime closedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new DailyBalance(id, date, openingBalance, totalCredits, totalDebits,
                closingBalance, status, closedAt, createdAt, updatedAt);
    }

    public DailyBalance withAddedCredit(Money credit) {
        if (status == BalanceStatus.CLOSED) {
            throw new BalanceAlreadyClosedException(date);
        }
        if (credit == null || !credit.isPositive()) {
            throw new ValidationException("Credit amount must be positive");
        }
        Money newCredits = totalCredits.add(credit);
        Money newClosing = calculateClosingBalance(openingBalance, newCredits, totalDebits);
        return new DailyBalance(id, date, openingBalance, newCredits, totalDebits,
                newClosing, status, closedAt, createdAt, LocalDateTime.now());
    }

    public DailyBalance withAddedDebit(Money debit) {
        if (status == BalanceStatus.CLOSED) {
            throw new BalanceAlreadyClosedException(date);
        }
        if (debit == null || !debit.isNegative()) {
            throw new ValidationException("Debit amount must be negative");
        }
        Money newDebits = totalDebits.add(debit);
        Money newClosing = calculateClosingBalance(openingBalance, totalCredits, newDebits);
        return new DailyBalance(id, date, openingBalance, totalCredits, newDebits,
                newClosing, status, closedAt, createdAt, LocalDateTime.now());
    }

    public DailyBalance close() {
        if (status == BalanceStatus.CLOSED) {
            throw new BalanceAlreadyClosedException(date);
        }
        LocalDateTime now = LocalDateTime.now();
        return new DailyBalance(id, date, openingBalance, totalCredits, totalDebits,
                closingBalance, BalanceStatus.CLOSED, now, createdAt, now);
    }

    public DailyBalance reopen() {
        if (status == BalanceStatus.OPEN) {
            throw new BalanceAlreadyOpenException(date);
        }
        return new DailyBalance(id, date, openingBalance, totalCredits, totalDebits,
                closingBalance, BalanceStatus.OPEN, null, createdAt, LocalDateTime.now());
    }

    public DailyBalance recalculate() {
        Money newClosing = calculateClosingBalance(openingBalance, totalCredits, totalDebits);
        return new DailyBalance(id, date, openingBalance, totalCredits, totalDebits,
                newClosing, status, closedAt, createdAt, LocalDateTime.now());
    }

    private static Money calculateClosingBalance(Money opening, Money credits, Money debits) {
        return opening.add(credits).add(debits);
    }

    public UUID id() { return id; }
    public LocalDate date() { return date; }
    public Money openingBalance() { return openingBalance; }
    public Money totalCredits() { return totalCredits; }
    public Money totalDebits() { return totalDebits; }
    public Money closingBalance() { return closingBalance; }
    public BalanceStatus status() { return status; }
    public LocalDateTime closedAt() { return closedAt; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
