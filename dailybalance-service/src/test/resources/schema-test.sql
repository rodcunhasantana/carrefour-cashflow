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