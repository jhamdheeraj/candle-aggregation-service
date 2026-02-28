package com.trading.candle.aggregator.service;

import java.util.Map;

public interface CandleHistoryService {
    Map<String, Object> getCandleHistory(String symbol, String interval, long from, long to);
}
