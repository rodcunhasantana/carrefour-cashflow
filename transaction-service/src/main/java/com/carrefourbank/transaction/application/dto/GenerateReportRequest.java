package com.carrefourbank.transaction.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateReportRequest(
        @NotBlank(message = "Report type is required") String reportType,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        String format
) {
    public GenerateReportRequest {
        if (format == null || format.isBlank()) {
            format = "PDF";
        }
    }
}
