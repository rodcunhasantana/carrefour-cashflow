package com.carrefourbank.dailybalance.infrastructure.adapter.persistence;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.dailybalance.domain.model.DailyBalanceTransaction;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceTransactionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDailyBalanceTransactionRepository implements DailyBalanceTransactionRepository {

    private final JdbcTemplate jdbc;

    public JdbcDailyBalanceTransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(DailyBalanceTransaction entry) {
        jdbc.update(
                """
                INSERT INTO daily_balance_transactions
                    (id, balance_id, transaction_id, event_id, transaction_type, amount, currency, applied_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                entry.id().toString(),
                entry.balanceId().toString(),
                entry.transactionId(),
                entry.eventId(),
                entry.type().name(),
                entry.amount().amount(),
                entry.amount().currency().name(),
                entry.appliedAt());
    }

    @Override
    public List<DailyBalanceTransaction> findByBalanceId(UUID balanceId) {
        return jdbc.query(
                """
                SELECT id, balance_id, transaction_id, event_id, transaction_type, amount, currency, applied_at
                FROM daily_balance_transactions
                WHERE balance_id = ?
                ORDER BY applied_at ASC
                """,
                this::mapRow,
                balanceId.toString());
    }

    private DailyBalanceTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DailyBalanceTransaction.restore(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("balance_id")),
                rs.getString("transaction_id"),
                rs.getString("event_id"),
                TransactionType.valueOf(rs.getString("transaction_type")),
                Money.of(rs.getBigDecimal("amount"), Currency.valueOf(rs.getString("currency"))),
                rs.getTimestamp("applied_at").toLocalDateTime());
    }
}
