package com.carrefourbank.transaction.application.command;

import com.carrefourbank.common.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreateCommand(
        TransactionType type,
        BigDecimal amount,
        LocalDate date,
        String description
) {}
