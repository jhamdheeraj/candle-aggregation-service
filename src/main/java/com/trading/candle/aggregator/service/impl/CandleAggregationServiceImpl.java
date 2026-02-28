package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.service.CandlePersistenceService;
import com.trading.candle.aggregator.util.CandleIntervalUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

@Service
public class CandleAggregationServiceImpl implements CandleAggregationService {

    private static final List<String> SUPPORTED_INTERVALS = List.of("1s", "1m");
    private static final long FLUSH_RATE_MS = 1000;
    private static final double PRICE_CALCULATION_DIVISOR = 2.0;

    private final ConcurrentMap<String, CandleEntity> activeCandles = new ConcurrentHashMap<>();

    private final CandleRepository candleRepository;
    private final CandlePersistenceService persistenceService;
    private final Executor taskExecutor;

    public CandleAggregationServiceImpl(CandleRepository candleRepository,
                               CandlePersistenceService persistenceService,
                               @Qualifier("candleAggregationExecutor") Executor taskExecutor) {
        this.candleRepository = candleRepository;
        this.persistenceService = persistenceService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    @Async
    public CompletableFuture<Void> processEvent(BidAskEvent event) {
        List<CompletableFuture<Void>> futures = SUPPORTED_INTERVALS.stream()
                .map(interval -> CompletableFuture.runAsync(() -> {
                    processEventForInterval(event, interval);
                }, taskExecutor))
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Scheduled(fixedRate = FLUSH_RATE_MS)
    @Async
    @Transactional
    public CompletableFuture<Void> flushToDatabase() {

        if (activeCandles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CandleEntity> candlesToSave = new java.util.ArrayList<>(activeCandles.values());
        
        return persistenceService.persistCandles(candlesToSave)
                .thenRun(activeCandles::clear);
    }

    private void processEventForInterval(BidAskEvent event, String interval) {
        long alignedTime = CandleIntervalUtil.alignTimeWithDelay(event.timestamp(), interval);
        String key = generateCandleKey(event.symbol(), interval, alignedTime);
        double price = calculateMidPrice(event.bid(), event.ask());
        
        activeCandles.compute(key, (k, existing) -> {
            if (existing == null) {
                return createNewCandle(event.symbol(), interval, alignedTime, price);
            } else {
                return updateExistingCandle(existing, price);
            }
        });
    }

    private String generateCandleKey(String symbol, String interval, long alignedTime) {
        return symbol + "_" + interval + "_" + alignedTime;
    }

    private double calculateMidPrice(double bid, double ask) {
        return (bid + ask) / PRICE_CALCULATION_DIVISOR;
    }

    private CandleEntity createNewCandle(String symbol, String interval, long alignedTime, double price) {
        CandleEntity candle = new CandleEntity();
        candle.setSymbol(symbol);
        candle.setCandleInterval(interval);
        candle.setOpenTime(alignedTime);
        candle.setOpenPrice(price);
        candle.setHighPrice(price);
        candle.setLowPrice(price);
        candle.setClosePrice(price);
        candle.setVolume(1);
        return candle;
    }

    private CandleEntity updateExistingCandle(CandleEntity existing, double price) {
        existing.setHighPrice(Math.max(existing.getHighPrice(), price));
        existing.setLowPrice(Math.min(existing.getLowPrice(), price));
        existing.setClosePrice(price);
        existing.setVolume(existing.getVolume() + 1);
        return existing;
    }
}
