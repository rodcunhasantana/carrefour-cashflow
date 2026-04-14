package com.carrefourbank.transaction.application.dto;

import java.time.LocalDateTime;

public record ReportStatusResponse(
        String reportId,
        String status,
        String statusCheckUrl,
        LocalDateTime estimatedCompletionTime
) {}
