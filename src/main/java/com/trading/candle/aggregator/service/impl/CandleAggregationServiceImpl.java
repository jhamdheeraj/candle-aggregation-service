package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.util.CandleIntervalUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CandleAggregationServiceImpl implements CandleAggregationService {

    private final ConcurrentMap<String, CandleEntity> activeCandles = new ConcurrentHashMap<>();

    private static final List<String> INTERVALS = List.of("1s", "1m");

    private final CandleRepository candleRepository;

    CandleAggregationServiceImpl(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    @Override
    public void processEvent(BidAskEvent event) {
        for (String interval : INTERVALS) {

            long alignedTime = CandleIntervalUtil.alignTime(event.timestamp(), interval);

            String key = event.symbol() + "_" + interval + "_" + alignedTime;

            activeCandles.compute(key, (k, existing) -> {

                double price = (event.bid() + event.ask()) / 2.0;

                if (existing == null) {
                    CandleEntity candle = new CandleEntity();
                    candle.setSymbol(event.symbol());
                    candle.setCandleInterval(interval);
                    candle.setOpenTime(alignedTime);
                    candle.setOpenPrice(price);
                    candle.setHighPrice(price);
                    candle.setLowPrice(price);
                    candle.setClosePrice(price);
                    candle.setVolume(1);
                    return candle;
                } else {
                    existing.setHighPrice(Math.max(existing.getHighPrice(), price));
                    existing.setLowPrice(Math.min(existing.getLowPrice(), price));
                    existing.setClosePrice(price);
                    existing.setVolume(existing.getVolume() + 1);
                    return existing;
                }
            });
        }
    }

    @Scheduled(fixedRate = 5000)
    public void flushToDatabase() {

        if (activeCandles.isEmpty()) {
            return;
        }

        for (CandleEntity candle : activeCandles.values()) {

            candleRepository.findBySymbolAndCandleIntervalAndOpenTime(
                    candle.getSymbol(),
                    candle.getCandleInterval(),
                    candle.getOpenTime()
            ).ifPresentOrElse(existing -> {

                // UPDATE existing candle
                existing.setHighPrice(
                        Math.max(existing.getHighPrice(), candle.getHighPrice()));
                existing.setLowPrice(
                        Math.min(existing.getLowPrice(), candle.getLowPrice()));
                existing.setClosePrice(candle.getClosePrice());
                existing.setVolume(existing.getVolume() + candle.getVolume());

                candleRepository.save(existing);

            }, () -> {
                // INSERT new candle
                candleRepository.save(candle);
            });
        }

        activeCandles.clear();
    }
}
