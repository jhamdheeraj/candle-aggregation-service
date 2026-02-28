package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandlePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class CandlePersistenceServiceImpl implements CandlePersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(CandlePersistenceServiceImpl.class);

    private final CandleRepository candleRepository;
    private final Executor taskExecutor;
    private final ApplicationContext applicationContext;

    public CandlePersistenceServiceImpl(CandleRepository candleRepository, 
                                       @Qualifier("candleAggregationExecutor") Executor taskExecutor,
                                       ApplicationContext applicationContext) {
        this.candleRepository = candleRepository;
        this.taskExecutor = taskExecutor;
        this.applicationContext = applicationContext;
    }

    @Override
    public CompletableFuture<Void> persistCandles(List<CandleEntity> candles) {
        if (candles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                CandlePersistenceService proxy = applicationContext.getBean(CandlePersistenceService.class);
                proxy.persistCandlesTransactional(candles);
                logger.info("Successfully persisted {} candles", candles.size());
            } catch (Exception e) {
                logger.error("Failed to persist candles: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to persist candles", e);
            }
        }, taskExecutor);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void persistCandlesTransactional(List<CandleEntity> candles) {
        persistCandlesBulk(candles);
    }

    private void persistCandlesBulk(List<CandleEntity> candles) {
        // Group candles by symbol and interval for efficient bulk operations
        Map<String, List<CandleEntity>> groupedCandles = candles.stream()
                .collect(HashMap::new, 
                        (map, candle) -> map.computeIfAbsent(
                                candle.getSymbol() + "_" + candle.getCandleInterval(), 
                                k -> new ArrayList<>()).add(candle),
                        HashMap::putAll);

        List<CandleEntity> candlesToInsert = new ArrayList<>();
        
        for (List<CandleEntity> group : groupedCandles.values()) {
            // Find existing candles in bulk
            List<Long> openTimes = group.stream()
                    .map(CandleEntity::getOpenTime)
                    .toList();
            
            String symbol = group.get(0).getSymbol();
            String interval = group.get(0).getCandleInterval();
            
            List<CandleEntity> existingCandles = candleRepository
                    .findBySymbolAndCandleIntervalAndOpenTimeIn(symbol, interval, openTimes);
            
            Map<Long, CandleEntity> existingMap = existingCandles.stream()
                    .collect(HashMap::new, 
                            (map, candle) -> map.put(candle.getOpenTime(), candle),
                            HashMap::putAll);
            
            // Process each candle in the group
            for (CandleEntity candle : group) {
                CandleEntity existing = existingMap.get(candle.getOpenTime());
                if (existing != null) {
                    // Update existing candle with bulk query
                    int updated = candleRepository.updateCandleAggregation(
                            existing.getId(),
                            candle.getHighPrice(),
                            candle.getLowPrice(),
                            candle.getClosePrice(),
                            candle.getVolume()
                    );
                    if (updated == 0) {
                        logger.warn("No rows updated for candle ID: {}", existing.getId());
                    }
                } else {
                    // Mark for bulk insert
                    candlesToInsert.add(candle);
                }
            }
        }
        
        // Bulk insert new candles
        if (!candlesToInsert.isEmpty()) {
            candleRepository.saveAll(candlesToInsert);
        }
    }
}
