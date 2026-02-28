package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandlePersistenceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class CandlePersistenceServiceImpl implements CandlePersistenceService {

    private final CandleRepository candleRepository;
    private final Executor taskExecutor;

    public CandlePersistenceServiceImpl(CandleRepository candleRepository, 
                                       @Qualifier("candleAggregationExecutor") Executor taskExecutor) {
        this.candleRepository = candleRepository;
        this.taskExecutor = taskExecutor;
    }

    @Override
    @Transactional
    public CompletableFuture<Void> persistCandles(List<CandleEntity> candles) {
        if (candles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            for (CandleEntity candle : candles) {
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
        }, taskExecutor);
    }
}
