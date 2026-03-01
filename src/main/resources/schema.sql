CREATE TABLE candles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    candle_interval VARCHAR(10) NOT NULL,
    open_time BIGINT NOT NULL,
    open_price DOUBLE,
    high_price DOUBLE,
    low_price DOUBLE,
    close_price DOUBLE,
    volume BIGINT,
    CONSTRAINT uk_symbol_interval_time UNIQUE (symbol, candle_interval, open_time)
);

-- Indexes for query optimization
CREATE INDEX idx_symbol_interval_time ON candles (symbol, candle_interval, open_time);
CREATE INDEX idx_open_time ON candles (open_time);