package com.carrefourbank.dailybalance.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExportBalanceRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String destination
) {}
