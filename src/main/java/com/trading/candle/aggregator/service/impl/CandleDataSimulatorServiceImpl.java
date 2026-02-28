package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.service.CandleDataSimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
public class CandleDataSimulatorServiceImpl implements CandleDataSimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(CandleDataSimulatorServiceImpl.class);

    private final Random random = new Random();
    private final CandleAggregationService candleAggregationService;

    public CandleDataSimulatorServiceImpl(CandleAggregationService candleAggregationService) {
        this.candleAggregationService = candleAggregationService;
    }

    protected Random getRandom() {
        return random;
    }

    @Override
    @Scheduled(fixedRate = 10)
    public void generateEvent() {
        try {
            String symbol = getRandom().nextBoolean() ? "BTC-USD" : "ETH-USD";
            double base = symbol.equals("BTC-USD") ? 30000 : 2000;
            double price = base + getRandom().nextDouble() * 100;

            BidAskEvent event = new BidAskEvent(
                    symbol,
                    price - 1,
                    price + 1,
                    Instant.now().getEpochSecond()
            );

            logger.debug("Generated event: {}", event);
            processEventAsync(event);
        } catch (Exception e) {
            logger.error("Error generating event: {}", e.getMessage(), e);
        }
    }

    @Async
    public void processEventAsync(BidAskEvent event) {
        try {
            candleAggregationService.processEvent(event);
        } catch (Exception e) {
            logger.error("Error processing event asynchronously: {}", e.getMessage(), e);
        }
    }
}
