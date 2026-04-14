package com.carrefourbank.transaction.application.dto;

import com.carrefourbank.common.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull(message = "Type is required") TransactionType type,
        @NotNull(message = "Amount is required") BigDecimal amount,
        @NotNull(message = "Date is required") LocalDate date,
        @NotBlank(message = "Description is required")
        @Size(min = 3, max = 100, message = "Description must be between 3 and 100 characters")
        String description
) {}
