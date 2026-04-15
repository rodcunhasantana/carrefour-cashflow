package com.carrefourbank.dailybalance.application.dto;

import java.time.LocalDateTime;

public record DailyBalanceTransactionDTO(
        String id,
        String transactionId,
        String eventId,
        String transactionType,
        String amount,
        String currency,
        LocalDateTime appliedAt
) {}
