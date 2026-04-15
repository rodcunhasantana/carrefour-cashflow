package com.carrefourbank.transaction.infrastructure.adapter.pubsub.event;

import java.time.LocalDate;

public record PeriodClosedEventData(String balanceId, LocalDate date) {}
