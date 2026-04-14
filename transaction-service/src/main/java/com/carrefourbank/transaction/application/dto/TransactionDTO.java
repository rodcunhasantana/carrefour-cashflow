package com.carrefourbank.transaction.application.dto;

import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.transaction.domain.model.TransactionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionDTO(
        String id,
        TransactionType type,
        String amount,
        String currency,
        LocalDate date,
        String description,
        LocalDateTime createdAt,
        TransactionStatus status,
        boolean reversal,
        String originalTransactionId
) {}
