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
    UNIQUE(symbol, candle_interval, open_time)
);