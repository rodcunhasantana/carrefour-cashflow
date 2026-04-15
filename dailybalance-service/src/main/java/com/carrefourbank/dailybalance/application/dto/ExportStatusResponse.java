package com.carrefourbank.dailybalance.application.dto;

import java.time.LocalDateTime;

public record ExportStatusResponse(
        String exportId,
        String status,
        String statusCheckUrl,
        LocalDateTime estimatedCompletionTime
) {}
