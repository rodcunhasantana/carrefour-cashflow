package com.carrefourbank.dailybalance.application.dto;

import java.util.List;

public record DailyBalancePageResponse(List<DailyBalanceDTO> content, PageableInfo pageable) {
    public record PageableInfo(int pageNumber, int pageSize, long totalElements, int totalPages) {}
}
