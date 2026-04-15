package com.carrefourbank.dailybalance.application.dto;

public record RecalculateResponse(DailyBalanceDTO balance, RecalculationDetails recalculationDetails) {
    public record RecalculationDetails(
            String previousCredits,
            String previousDebits,
            String previousClosingBalance,
            String newClosingBalance
    ) {}
}
