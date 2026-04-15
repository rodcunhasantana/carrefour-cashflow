-- Schema para testes do serviço DailyBalance
DROP TABLE IF EXISTS daily_balances;

CREATE TABLE daily_balances (
    id VARCHAR(36) PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    opening_balance DECIMAL(19,4) NOT NULL,
    total_credits DECIMAL(19,4) NOT NULL,
    total_debits DECIMAL(19,4) NOT NULL,
    closing_balance DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_dailybalance_date ON daily_balances (date);
CREATE INDEX idx_dailybalance_status ON daily_balances (status);

DROP TABLE IF EXISTS processed_events;
CREATE TABLE processed_events (
    event_id    VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS daily_balance_transactions;
CREATE TABLE daily_balance_transactions (
    id               VARCHAR(36) PRIMARY KEY,
    balance_id       VARCHAR(36) NOT NULL,
    transaction_id   VARCHAR(36) NOT NULL,
    event_id         VARCHAR(36) NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    amount           DECIMAL(19,4) NOT NULL,
    currency         VARCHAR(3) NOT NULL,
    applied_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_dbt_balance_id ON daily_balance_transactions (balance_id);