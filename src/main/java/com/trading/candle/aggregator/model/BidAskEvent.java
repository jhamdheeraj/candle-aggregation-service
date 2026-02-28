package com.trading.candle.aggregator.model;

public record BidAskEvent(
        String symbol,
        double bid,
        double ask,
        long timestamp
) {}