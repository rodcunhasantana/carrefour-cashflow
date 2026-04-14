package com.carrefourbank.transaction.infrastructure.adapter.persistence;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.transaction.domain.model.Transaction;
import com.carrefourbank.transaction.domain.model.TransactionStatus;
import com.carrefourbank.transaction.domain.port.TransactionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTransactionRepository implements TransactionRepository {

    private final JdbcTemplate jdbc;

    public JdbcTransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Transaction save(Transaction transaction) {
        String checkSql = "SELECT COUNT(1) FROM transactions WHERE id = ?";
        Integer count = jdbc.queryForObject(checkSql, Integer.class, transaction.id().toString());
        if (count != null && count > 0) {
            update(transaction);
        } else {
            insert(transaction);
        }
        return transaction;
    }

    private void insert(Transaction transaction) {
        String sql = """
                INSERT INTO transactions
                    (id, type, amount, currency, date, description, created_at, status, is_reversal, original_transaction_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbc.update(sql,
                transaction.id().toString(),
                transaction.type().name(),
                transaction.amount().amount(),
                transaction.amount().currency().name(),
                transaction.date(),
                transaction.description(),
                transaction.createdAt(),
                transaction.status().name(),
                transaction.isReversal(),
                transaction.originalTransactionId() != null ? transaction.originalTransactionId().toString() : null);
    }

    private void update(Transaction transaction) {
        String sql = "UPDATE transactions SET status = ? WHERE id = ?";
        jdbc.update(sql, transaction.status().name(), transaction.id().toString());
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        List<Transaction> results = jdbc.query(sql, new TransactionRowMapper(), id.toString());
        return results.stream().findFirst();
    }

    @Override
    public boolean existsReversalFor(UUID originalId) {
        String sql = "SELECT COUNT(1) FROM transactions WHERE original_transaction_id = ? AND is_reversal = TRUE";
        Integer count = jdbc.queryForObject(sql, Integer.class, originalId.toString());
        return count != null && count > 0;
    }

    @Override
    public List<Transaction> findAll(LocalDate startDate, LocalDate endDate, TransactionType type, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1");
        appendFilters(sql, params, startDate, endDate, type);
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((long) page * size);
        return jdbc.query(sql.toString(), new TransactionRowMapper(), params.toArray());
    }

    @Override
    public long count(LocalDate startDate, LocalDate endDate, TransactionType type) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM transactions WHERE 1=1");
        appendFilters(sql, params, startDate, endDate, type);
        Long count = jdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    private void appendFilters(StringBuilder sql, List<Object> params, LocalDate startDate, LocalDate endDate, TransactionType type) {
        if (startDate != null) {
            sql.append(" AND date >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND date <= ?");
            params.add(endDate);
        }
        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.name());
        }
    }

    private static class TransactionRowMapper implements RowMapper<Transaction> {
        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = UUID.fromString(rs.getString("id"));
            TransactionType type = TransactionType.valueOf(rs.getString("type"));
            Money amount = Money.of(rs.getBigDecimal("amount"), Currency.valueOf(rs.getString("currency")));
            LocalDate date = rs.getObject("date", LocalDate.class);
            String description = rs.getString("description");
            LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
            TransactionStatus status = TransactionStatus.valueOf(rs.getString("status"));
            boolean isReversal = rs.getBoolean("is_reversal");
            String originalIdStr = rs.getString("original_transaction_id");
            UUID originalId = originalIdStr != null ? UUID.fromString(originalIdStr) : null;

            return Transaction.restore(id, type, amount, date, description, status, createdAt, isReversal, originalId);
        }
    }
}
