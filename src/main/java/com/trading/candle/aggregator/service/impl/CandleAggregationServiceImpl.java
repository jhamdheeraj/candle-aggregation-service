package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.config.ApplicationLifecycleManager;
import com.trading.candle.aggregator.config.CandleAggregationProperties;
import com.trading.candle.aggregator.controller.HealthController;
import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.service.CandlePersistenceService;
import com.trading.candle.aggregator.util.CandleIntervalUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CandleAggregationServiceImpl.class);

    private final CandleAggregationProperties properties;

    private final ConcurrentMap<String, CandleEntity> activeCandles = new ConcurrentHashMap<>();
    private List<String> supportedIntervals;

    private final CandleRepository candleRepository;
    private final CandlePersistenceService persistenceService;
    private final Executor taskExecutor;
    private final ApplicationLifecycleManager lifecycleManager;
    private final HealthController healthController;

    public CandleAggregationServiceImpl(CandleRepository candleRepository,
                                        CandlePersistenceService persistenceService,
                                        @Qualifier("candleAggregationExecutor") Executor taskExecutor,
                                        CandleAggregationProperties properties,
                                        ApplicationLifecycleManager lifecycleManager,
                                        HealthController healthController) {
        this.candleRepository = candleRepository;
        this.persistenceService = persistenceService;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
        this.lifecycleManager = lifecycleManager;
        this.healthController = healthController;
    }

    @PostConstruct
    public void init() {
        this.supportedIntervals = properties.getIntervals();
        logger.info("Initialized candle aggregation with intervals: {}", supportedIntervals);
    }

    @Override
    @Async
    public CompletableFuture<Void> processEvent(BidAskEvent event) {
        logger.info("Received new event: symbol={}, bid={}, ask={}, timestamp={}",
                event.symbol(), event.bid(), event.ask(), event.timestamp());

        // Reject new events during shutdown
        if (lifecycleManager.isShuttingDown()) {
            logger.warn("Rejecting event during shutdown: symbol={}", event.symbol());
            return CompletableFuture.completedFuture(null);
        }

        try {
            List<CompletableFuture<Void>> futures = supportedIntervals.stream()
                    .map(interval -> CompletableFuture.runAsync(() -> {
                        try {
                            processEventForInterval(event, interval);
                        } catch (Exception e) {
                            logger.error("Error processing event for interval {}: {}", interval, e.getMessage(), e);
                            healthController.setAggregationStatus(false);
                        }
                    }, taskExecutor))
                    .toList();

            // Update health indicator with successful processing
            healthController.updateLastCandleProcessed();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
            healthController.setAggregationStatus(false);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Scheduled(fixedRateString = "#{@candleAggregationProperties.flushRateMs}")
    @Async
    @Transactional
    public CompletableFuture<Void> flushToDatabase() {

        // Skip flush during shutdown - will be handled by @PreDestroy
        if (lifecycleManager.isShuttingDown()) {
            return CompletableFuture.completedFuture(null);
        }

        if (activeCandles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Flushing {} candles to database", activeCandles.size());
        List<CandleEntity> candlesToSave = new java.util.ArrayList<>(activeCandles.values());

        return persistenceService.persistCandles(candlesToSave)
                .thenRun(() -> {
                    logger.info("Successfully flushed {} candles", candlesToSave.size());
                    activeCandles.clear();
                    healthController.setPersistenceStatus(true);
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to flush candles to database: {}", throwable.getMessage(), throwable);
                    healthController.setPersistenceStatus(false);
                    return null;
                });
    }

    private void processEventForInterval(BidAskEvent event, String interval) {
        long alignedTime = CandleIntervalUtil.alignTimeWithDelay(event.timestamp(), interval);
        String key = generateCandleKey(event.symbol(), interval, alignedTime);
        double price = calculateMidPrice(event.bid(), event.ask());

        activeCandles.compute(key, (k, existing) -> {
            if (existing == null) {
                logger.info("Creating new candle: symbol={}, interval={}, time={}, price={}",
                        event.symbol(), interval, alignedTime, price);
                return createNewCandle(event.symbol(), interval, alignedTime, price);
            } else {
                logger.info("Updating existing candle: symbol={}, interval={}, time={}, price={}",
                        event.symbol(), interval, alignedTime, price);
                return updateExistingCandle(existing, price);
            }
        });
    }

    private String generateCandleKey(String symbol, String interval, long alignedTime) {
        return symbol + "_" + interval + "_" + alignedTime;
    }

    private double calculateMidPrice(double bid, double ask) {
        return (bid + ask) / properties.getProcessing().getPriceCalculationDivisor();
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
        healthController.setAggregationStatus(false);

        if (!activeCandles.isEmpty()) {
            logger.info("Flushing {} remaining candles before shutdown", activeCandles.size());
            try {
                List<CandleEntity> candlesToSave = new java.util.ArrayList<>(activeCandles.values());
                persistenceService.persistCandles(candlesToSave).get();
                logger.info("Successfully flushed {} candles on shutdown", candlesToSave.size());
                activeCandles.clear();
                healthController.setPersistenceStatus(true);
            } catch (Exception e) {
                logger.error("Failed to flush candles during shutdown: {}", e.getMessage(), e);
                healthController.setPersistenceStatus(false);
            }
        }

        logger.info("Candle aggregation service shutdown completed");
    }
}
