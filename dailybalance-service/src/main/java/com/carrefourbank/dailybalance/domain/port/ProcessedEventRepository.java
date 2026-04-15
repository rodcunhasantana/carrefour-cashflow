package com.carrefourbank.dailybalance.domain.port;

public interface ProcessedEventRepository {

    /**
     * Marks an event as processed.
     * @return true if the event was newly registered, false if it was already processed (duplicate)
     */
    boolean markAsProcessed(String eventId);
}
