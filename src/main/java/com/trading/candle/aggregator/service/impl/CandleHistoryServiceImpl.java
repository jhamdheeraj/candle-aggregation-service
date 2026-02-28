package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandleHistoryService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CandleHistoryServiceImpl implements CandleHistoryService {

    private final CandleRepository candleRepository;

    CandleHistoryServiceImpl(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    public Map<String, Object> getCandleHistory(
            String symbol,
            String interval,
            long from,
            long to) {

        var candles = candleRepository
                .findBySymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                        symbol, interval, from, to);

        Map<String, Object> response = new HashMap<>();
        response.put("s", "ok");
        response.put("t", candles.stream().map(c -> c.getOpenTime()).toList());
        response.put("o", candles.stream().map(c -> c.getOpenPrice()).toList());
        response.put("h", candles.stream().map(c -> c.getHighPrice()).toList());
        response.put("l", candles.stream().map(c -> c.getLowPrice()).toList());
        response.put("c", candles.stream().map(c -> c.getClosePrice()).toList());
        response.put("v", candles.stream().map(c -> c.getVolume()).toList());

        return response;
    }
}
