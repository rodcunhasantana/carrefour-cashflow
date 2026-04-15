package com.carrefourbank.dailybalance.infrastructure.adapter.pubsub.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreatedEventData(
        String transactionId,
        String type,
        BigDecimal amount,
        String currency,
        LocalDate date,
        String description
) {}
