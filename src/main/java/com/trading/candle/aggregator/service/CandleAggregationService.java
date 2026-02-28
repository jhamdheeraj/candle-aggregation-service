package com.trading.candle.aggregator.service;

import com.trading.candle.aggregator.model.BidAskEvent;
import java.util.concurrent.CompletableFuture;

public interface CandleAggregationService {
    public CompletableFuture<Void> processEvent(BidAskEvent event);
}
