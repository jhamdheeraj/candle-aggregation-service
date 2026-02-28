package com.trading.candle.aggregator.service;

import com.trading.candle.aggregator.model.BidAskEvent;
import org.springframework.stereotype.Service;

@Service
public class CandleAggregationServiceImpl implements CandleAggregationService {
    @Override
    public void processEvent(BidAskEvent event) {

    }
}
