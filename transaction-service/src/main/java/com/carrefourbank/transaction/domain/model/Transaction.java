package com.carrefourbank.transaction.domain.model;

import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final TransactionType type;
    private final Money amount;
    private final LocalDate date;
    private final String description;
    private final TransactionStatus status;
    private final LocalDateTime createdAt;
    private final boolean reversal;
    private final UUID originalTransactionId;

    private Transaction(
            UUID id,
            TransactionType type,
            Money amount,
            LocalDate date,
            String description,
            TransactionStatus status,
            LocalDateTime createdAt,
            boolean reversal,
            UUID originalTransactionId) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.reversal = reversal;
        this.originalTransactionId = originalTransactionId;
    }

    public static Transaction create(TransactionType type, Money amount, LocalDate date, String description) {
        if (type == null) {
            throw new ValidationException("Transaction type is required");
        }
        if (amount == null) {
            throw new ValidationException("Amount is required");
        }
        if (date == null) {
            throw new ValidationException("Date is required");
        }
        if (description == null || description.isBlank()) {
            throw new ValidationException("Description is required");
        }
        if (type == TransactionType.CREDIT && !amount.isPositive()) {
            throw new ValidationException("Credit transactions must have a positive amount");
        }
        if (type == TransactionType.DEBIT && !amount.isNegative()) {
            throw new ValidationException("Debit transactions must have a negative amount");
        }
        return new Transaction(
                UUID.randomUUID(),
                type,
                amount,
                date,
                description,
                TransactionStatus.PENDING,
                LocalDateTime.now(),
                false,
                null);
    }

    public static Transaction createReversal(Transaction original, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reversal reason is required");
        }
        String reversalDescription = "Reversal: " + original.description + " - Reason: " + reason;
        // O estorno inverte o tipo e nega o valor:
        // CREDIT +500 → DEBIT -500  |  DEBIT -200 → CREDIT +200
        TransactionType reversalType = original.type == TransactionType.CREDIT
                ? TransactionType.DEBIT
                : TransactionType.CREDIT;
        Money reversedAmount = original.amount.negate();
        return new Transaction(
                UUID.randomUUID(),
                reversalType,
                reversedAmount,
                original.date,
                reversalDescription,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                true,
                original.id);
    }

    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(
                this.id,
                this.type,
                this.amount,
                this.date,
                this.description,
                newStatus,
                this.createdAt,
                this.reversal,
                this.originalTransactionId);
    }

    public static Transaction restore(
            UUID id,
            TransactionType type,
            Money amount,
            LocalDate date,
            String description,
            TransactionStatus status,
            java.time.LocalDateTime createdAt,
            boolean reversal,
            UUID originalTransactionId) {
        return new Transaction(id, type, amount, date, description, status, createdAt, reversal, originalTransactionId);
    }

    public UUID id() { return id; }
    public TransactionType type() { return type; }
    public Money amount() { return amount; }
    public LocalDate date() { return date; }
    public String description() { return description; }
    public TransactionStatus status() { return status; }
    public LocalDateTime createdAt() { return createdAt; }
    public boolean isReversal() { return reversal; }
    public UUID originalTransactionId() { return originalTransactionId; }
}
