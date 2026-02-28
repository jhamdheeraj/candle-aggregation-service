package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandleAggregationService;
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
import java.util.stream.Collectors;

@Service
public class CandleAggregationServiceImpl implements CandleAggregationService {

    private final ConcurrentMap<String, CandleEntity> activeCandles = new ConcurrentHashMap<>();

    private static final List<String> INTERVALS = List.of("1s", "1m");

    private final CandleRepository candleRepository;
    private final Executor taskExecutor;

    CandleAggregationServiceImpl(CandleRepository candleRepository, @Qualifier("candleAggregationExecutor") Executor taskExecutor) {
        this.candleRepository = candleRepository;
        this.taskExecutor = taskExecutor;
    }

    @Override
    @Async
    public CompletableFuture<Void> processEvent(BidAskEvent event) {
        List<CompletableFuture<Void>> futures = INTERVALS.stream()
                .map(interval -> CompletableFuture.runAsync(() -> {
                    long alignedTime = CandleIntervalUtil.alignTimeWithDelay(event.timestamp(), interval);
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
                }, taskExecutor))
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Scheduled(fixedRate = 1000)
    @Async
    @Transactional
    public CompletableFuture<Void> flushToDatabase() {

        if (activeCandles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CandleEntity> candlesToSave = new java.util.ArrayList<>(activeCandles.values());
        
        return CompletableFuture.runAsync(() -> {
            for (CandleEntity candle : candlesToSave) {
                candleRepository.findBySymbolAndCandleIntervalAndOpenTime(
                        candle.getSymbol(),
                        candle.getCandleInterval(),
                        candle.getOpenTime()
                ).ifPresentOrElse(existing -> {
                    existing.setHighPrice(Math.max(existing.getHighPrice(), candle.getHighPrice()));
                    existing.setLowPrice(Math.min(existing.getLowPrice(), candle.getLowPrice()));
                    existing.setClosePrice(candle.getClosePrice());
                    existing.setVolume(existing.getVolume() + candle.getVolume());
                    candleRepository.save(existing);
                }, () -> {
                    candleRepository.save(candle);
                });
            }
            
            activeCandles.clear();
        }, taskExecutor);
    }
}
