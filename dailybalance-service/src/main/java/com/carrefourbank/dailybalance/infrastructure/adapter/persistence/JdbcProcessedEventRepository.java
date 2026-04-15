package com.carrefourbank.dailybalance.infrastructure.adapter.persistence;

import com.carrefourbank.dailybalance.domain.port.ProcessedEventRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class JdbcProcessedEventRepository implements ProcessedEventRepository {

    private final JdbcTemplate jdbc;

    public JdbcProcessedEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markAsProcessed(String eventId) {
        try {
            jdbc.update(
                    "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?)",
                    eventId, LocalDateTime.now());
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
