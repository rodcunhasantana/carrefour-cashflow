package com.carrefourbank.transaction.infrastructure.adapter.persistence;

import com.carrefourbank.transaction.domain.port.ClosedPeriodRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public class JdbcClosedPeriodRepository implements ClosedPeriodRepository {

    private final JdbcTemplate jdbc;

    public JdbcClosedPeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isDateClosed(LocalDate date) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM closed_periods WHERE date = ?",
                Integer.class, date);
        return count != null && count > 0;
    }

    @Override
    public void closeDate(LocalDate date) {
        jdbc.update(
                "INSERT INTO closed_periods (date, closed_at) VALUES (?, ?) ON CONFLICT (date) DO NOTHING",
                date, LocalDateTime.now());
    }
}
