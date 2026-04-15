package com.carrefourbank.dailybalance.infrastructure.adapter.pubsub.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionReversedEventData(
        String originalTransactionId,
        String reversalTransactionId,
        String reason,
        LocalDate date,
        BigDecimal amount,
        String currency,
        String type
) {}
