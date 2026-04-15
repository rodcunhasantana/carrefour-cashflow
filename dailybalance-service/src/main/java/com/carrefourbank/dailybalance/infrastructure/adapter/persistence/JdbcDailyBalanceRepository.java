package com.carrefourbank.dailybalance.infrastructure.adapter.persistence;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.dailybalance.domain.model.BalanceStatus;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDailyBalanceRepository implements DailyBalanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDailyBalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DailyBalance save(DailyBalance balance) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM daily_balances WHERE id = ?",
                Integer.class, balance.id().toString());

        if (count != null && count > 0) {
            jdbcTemplate.update(
                    "UPDATE daily_balances SET opening_balance = ?, total_credits = ?, total_debits = ?, " +
                    "closing_balance = ?, status = ?, closed_at = ?, updated_at = ? WHERE id = ?",
                    balance.openingBalance().amount(),
                    balance.totalCredits().amount(),
                    balance.totalDebits().amount(),
                    balance.closingBalance().amount(),
                    balance.status().name(),
                    balance.closedAt() != null ? Timestamp.valueOf(balance.closedAt()) : null,
                    Timestamp.valueOf(balance.updatedAt()),
                    balance.id().toString());
        } else {
            jdbcTemplate.update(
                    "INSERT INTO daily_balances (id, date, opening_balance, total_credits, total_debits, " +
                    "closing_balance, status, closed_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    balance.id().toString(),
                    balance.date(),
                    balance.openingBalance().amount(),
                    balance.totalCredits().amount(),
                    balance.totalDebits().amount(),
                    balance.closingBalance().amount(),
                    balance.status().name(),
                    balance.closedAt() != null ? Timestamp.valueOf(balance.closedAt()) : null,
                    Timestamp.valueOf(balance.createdAt()),
                    Timestamp.valueOf(balance.updatedAt()));
        }
        return balance;
    }

    @Override
    public Optional<DailyBalance> findById(UUID id) {
        List<DailyBalance> results = jdbcTemplate.query(
                "SELECT * FROM daily_balances WHERE id = ?",
                new DailyBalanceRowMapper(), id.toString());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<DailyBalance> findByDate(LocalDate date) {
        List<DailyBalance> results = jdbcTemplate.query(
                "SELECT * FROM daily_balances WHERE date = ?",
                new DailyBalanceRowMapper(), date);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<DailyBalance> findMostRecentClosedBefore(LocalDate date) {
        List<DailyBalance> results = jdbcTemplate.query(
                "SELECT * FROM daily_balances WHERE date < ? AND status = 'CLOSED' ORDER BY date DESC LIMIT 1",
                new DailyBalanceRowMapper(), date);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<DailyBalance> findAll(LocalDate startDate, LocalDate endDate, BalanceStatus status, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM daily_balances WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (startDate != null) { sql.append(" AND date >= ?"); params.add(startDate); }
        if (endDate != null)   { sql.append(" AND date <= ?"); params.add(endDate); }
        if (status != null)    { sql.append(" AND status = ?"); params.add(status.name()); }

        sql.append(" ORDER BY date DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((long) page * size);

        return jdbcTemplate.query(sql.toString(), new DailyBalanceRowMapper(), params.toArray());
    }

    @Override
    public long count(LocalDate startDate, LocalDate endDate, BalanceStatus status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM daily_balances WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (startDate != null) { sql.append(" AND date >= ?"); params.add(startDate); }
        if (endDate != null)   { sql.append(" AND date <= ?"); params.add(endDate); }
        if (status != null)    { sql.append(" AND status = ?"); params.add(status.name()); }

        Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return result != null ? result : 0L;
    }

    private static class DailyBalanceRowMapper implements RowMapper<DailyBalance> {
        @Override
        public DailyBalance mapRow(ResultSet rs, int rowNum) throws SQLException {
            Currency currency = Currency.BRL;
            return DailyBalance.restore(
                    UUID.fromString(rs.getString("id")),
                    rs.getDate("date").toLocalDate(),
                    Money.of(rs.getBigDecimal("opening_balance"), currency),
                    Money.of(rs.getBigDecimal("total_credits"), currency),
                    Money.of(rs.getBigDecimal("total_debits"), currency),
                    Money.of(rs.getBigDecimal("closing_balance"), currency),
                    BalanceStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("closed_at") != null ? rs.getTimestamp("closed_at").toLocalDateTime() : null,
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime());
        }
    }
}
