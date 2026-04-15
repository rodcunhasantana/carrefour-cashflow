package com.carrefourbank.dailybalance.infrastructure.web;

public record ErrorResponse(String code, String message, String timestamp) {}
