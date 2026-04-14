package com.carrefourbank.transaction.infrastructure.web;

public record ErrorResponse(
        String code,
        String message,
        String timestamp
) {}
