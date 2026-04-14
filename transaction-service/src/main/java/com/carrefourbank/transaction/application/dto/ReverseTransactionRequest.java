package com.carrefourbank.transaction.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReverseTransactionRequest(
        @NotBlank(message = "Reason is required")
        @Size(min = 3, max = 100, message = "Reason must be between 3 and 100 characters")
        String reason
) {}
