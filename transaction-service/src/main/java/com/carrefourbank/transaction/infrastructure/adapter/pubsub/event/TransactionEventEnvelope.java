package com.carrefourbank.transaction.infrastructure.adapter.pubsub.event;

public record TransactionEventEnvelope(
        String eventId,
        String eventType,
        String timestamp,
        String producer,
        Object data
) {}
