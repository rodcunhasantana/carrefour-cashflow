package com.carrefourbank.dailybalance.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ReopenBalanceRequest(
        @NotBlank String reason,
        @NotBlank String approvedBy
) {}
