package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.service.CandlePersistenceService;
import com.trading.candle.aggregator.util.CandleIntervalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

@Service
public class CandleAggregationServiceImpl implements CandleAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(CandleAggregationServiceImpl.class);
    
    @Value("${candle.aggregation.intervals:1s,1m}")
    private String intervalsConfig;
    
    @Value("${candle.aggregation.flush-rate-ms:1000}")
    private long flushRateMs;
    
    private static final double PRICE_CALCULATION_DIVISOR = 2.0;

    private final ConcurrentMap<String, CandleEntity> activeCandles = new ConcurrentHashMap<>();
    private List<String> supportedIntervals;

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

    @PostConstruct
    public void init() {
        this.supportedIntervals = Arrays.asList(intervalsConfig.split(","));
        logger.info("Initialized candle aggregation with intervals: {}", supportedIntervals);
    }

    @Override
    @Async
    public CompletableFuture<Void> processEvent(BidAskEvent event) {
        try {
            List<CompletableFuture<Void>> futures = supportedIntervals.stream()
                    .map(interval -> CompletableFuture.runAsync(() -> {
                        try {
                            processEventForInterval(event, interval);
                        } catch (Exception e) {
                            logger.error("Error processing event for interval {}: {}", interval, e.getMessage(), e);
                        }
                    }, taskExecutor))
                    .toList();
            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Scheduled(fixedRateString = "#{${candle.aggregation.flush-rate-ms:1000}}")
    @Async
    @Transactional
    public CompletableFuture<Void> flushToDatabase() {

        if (activeCandles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Flushing {} candles to database", activeCandles.size());
        List<CandleEntity> candlesToSave = new java.util.ArrayList<>(activeCandles.values());
        
        return persistenceService.persistCandles(candlesToSave)
                .thenRun(() -> {
                    logger.info("Successfully flushed {} candles", candlesToSave.size());
                    activeCandles.clear();
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to flush candles to database: {}", throwable.getMessage(), throwable);
                    return null;
                });
    }

    private void processEventForInterval(BidAskEvent event, String interval) {
        logger.info("Processing event: symbol={}, interval={}, timestamp={}, bid={}, ask={}", 
                   event.symbol(), interval, event.timestamp(), event.bid(), event.ask());
        
        long alignedTime = CandleIntervalUtil.alignTimeWithDelay(event.timestamp(), interval);
        String key = generateCandleKey(event.symbol(), interval, alignedTime);
        double price = calculateMidPrice(event.bid(), event.ask());
        
        activeCandles.compute(key, (k, existing) -> {
            if (existing == null) {
                logger.info("Creating new candle: symbol={}, interval={}, time={}, price={}", 
                           event.symbol(), interval, alignedTime, price);
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

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down candle aggregation service...");
        if (!activeCandles.isEmpty()) {
            logger.info("Flushing {} remaining candles before shutdown", activeCandles.size());
            try {
                List<CandleEntity> candlesToSave = new java.util.ArrayList<>(activeCandles.values());
                persistenceService.persistCandles(candlesToSave).get();
                logger.info("Successfully flushed {} candles on shutdown", candlesToSave.size());
                activeCandles.clear();
            } catch (Exception e) {
                logger.error("Failed to flush candles during shutdown: {}", e.getMessage(), e);
            }
        }
    }
}
