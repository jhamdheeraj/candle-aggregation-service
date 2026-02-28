package com.trading.candle.aggregator.model;

public record BidAskEvent(
        String symbol,
        double bid,
        double ask,
        long timestamp
) {
    public BidAskEvent {
        validateSymbol(symbol);
        validatePrices(bid, ask);
        validateTimestamp(timestamp);
    }
    
    private static void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
    }
    
    private static void validatePrices(double bid, double ask) {
        if (bid < 0) {
            throw new IllegalArgumentException("Bid must be non-negative, got: " + bid);
        }
        if (ask < 0) {
            throw new IllegalArgumentException("Ask must be non-negative, got: " + ask);
        }
        if (bid > ask) {
            throw new IllegalArgumentException("Bid cannot be greater than ask, bid: " + bid + ", ask: " + ask);
        }
    }
    
    private static void validateTimestamp(long timestamp) {
        if (timestamp <= 0) {
            throw new IllegalArgumentException("Timestamp must be positive, got: " + timestamp);
        }
    }
}