package com.carrefourbank.dailybalance.domain.model;

import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de auditoria imutável que associa um lançamento financeiro ao saldo
 * diário em que foi aplicado. Permite rastrear quais transações compõem um saldo.
 */
public class DailyBalanceTransaction {

    private final UUID id;
    private final UUID balanceId;
    private final String transactionId;
    private final String eventId;
    private final TransactionType type;
    private final Money amount;
    private final LocalDateTime appliedAt;

    private DailyBalanceTransaction(
            UUID id,
            UUID balanceId,
            String transactionId,
            String eventId,
            TransactionType type,
            Money amount,
            LocalDateTime appliedAt) {
        this.id = id;
        this.balanceId = balanceId;
        this.transactionId = transactionId;
        this.eventId = eventId;
        this.type = type;
        this.amount = amount;
        this.appliedAt = appliedAt;
    }

    public static DailyBalanceTransaction create(
            UUID balanceId,
            String transactionId,
            String eventId,
            TransactionType type,
            Money amount) {
        return new DailyBalanceTransaction(
                UUID.randomUUID(), balanceId, transactionId, eventId, type, amount, LocalDateTime.now());
    }

    public static DailyBalanceTransaction restore(
            UUID id,
            UUID balanceId,
            String transactionId,
            String eventId,
            TransactionType type,
            Money amount,
            LocalDateTime appliedAt) {
        return new DailyBalanceTransaction(id, balanceId, transactionId, eventId, type, amount, appliedAt);
    }

    public UUID id()            { return id; }
    public UUID balanceId()     { return balanceId; }
    public String transactionId() { return transactionId; }
    public String eventId()     { return eventId; }
    public TransactionType type() { return type; }
    public Money amount()       { return amount; }
    public LocalDateTime appliedAt() { return appliedAt; }
}
