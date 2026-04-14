package com.carrefourbank.transaction.application.dto;

import java.util.List;

public record TransactionPageResponse(
        List<TransactionDTO> content,
        PageableInfo pageable
) {
    public record PageableInfo(
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages
    ) {}
}
