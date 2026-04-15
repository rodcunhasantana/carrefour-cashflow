package com.carrefourbank.dailybalance.application.dto;

import com.carrefourbank.dailybalance.domain.model.BalanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyBalanceDTO(
        String id,
        LocalDate date,
        String openingBalance,
        String totalCredits,
        String totalDebits,
        String closingBalance,
        String currency,
        BalanceStatus status,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
