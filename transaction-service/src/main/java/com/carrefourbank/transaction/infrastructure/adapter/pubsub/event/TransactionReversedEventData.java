package com.carrefourbank.transaction.infrastructure.adapter.pubsub.event;

import java.time.LocalDate;

public record TransactionReversedEventData(
        String originalTransactionId,
        String reversalTransactionId,
        String reason,
        LocalDate date
) {}
