package com.trading.candle.aggregator.service;

import com.trading.candle.aggregator.model.BidAskEvent;

public interface CandleAggregationService {
    public void processEvent(BidAskEvent event);
}
