CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(10) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_reversal BOOLEAN DEFAULT FALSE,
    original_transaction_id VARCHAR(36)
);

CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions (date);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions (type);

CREATE TABLE IF NOT EXISTS closed_periods (
    date      DATE PRIMARY KEY,
    closed_at TIMESTAMP NOT NULL
);