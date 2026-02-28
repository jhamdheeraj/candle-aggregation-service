package com.trading.candle.aggregator.model;

public record Candle(
        long openTime,
        double open,
        double high,
        double low,
        double close,
        long volume
) {}