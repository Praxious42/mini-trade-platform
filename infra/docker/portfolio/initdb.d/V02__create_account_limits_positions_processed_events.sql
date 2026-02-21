CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS account_limits (
    account_id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    max_notional NUMERIC(19,4) NOT NULL DEFAULT 50000.0000,
    max_pos_per_symbol  NUMERIC(19,4) NOT NULL DEFAULT 1000.0000,
    margin_rate_fx NUMERIC(6,5) NOT NULL DEFAULT 0.02000,
    margin_rate_stock NUMERIC(6,5) NOT NULL DEFAULT 0.50000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_limits_positive
        CHECK (max_notional > 0 AND max_pos_per_symbol > 0),

    CONSTRAINT chk_margin_rates
        CHECK (margin_rate_fx > 0 AND margin_rate_fx <= 1
           AND margin_rate_stock > 0 AND margin_rate_stock <= 1)
);

CREATE TABLE IF NOT EXISTS positions (
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    symbol VARCHAR(32) NOT NULL,
    net_qty NUMERIC(19,4) NOT NULL,
    avg_price NUMERIC(19,5) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, symbol),

    CONSTRAINT chk_symbol_not_blank CHECK (length(trim(symbol)) > 0),
    CONSTRAINT chk_net_qty_reasonable CHECK (net_qty <> 0 OR avg_price = 0.00000),
    CONSTRAINT chk_avg_price_non_negative CHECK (avg_price >= 0.00000)
);

CREATE INDEX IF NOT EXISTS idx_positions_account ON positions(account_id);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
