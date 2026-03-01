package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
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
        response.put("t", candles.stream().map(CandleEntity::getOpenTime).toList());
        response.put("o", candles.stream().map(CandleEntity::getOpenPrice).toList());
        response.put("h", candles.stream().map(CandleEntity::getHighPrice).toList());
        response.put("l", candles.stream().map(CandleEntity::getLowPrice).toList());
        response.put("c", candles.stream().map(CandleEntity::getClosePrice).toList());
        response.put("v", candles.stream().map(CandleEntity::getVolume).toList());

        return response;
    }
}
