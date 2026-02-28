package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.service.CandleDataSimulatorService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
public class CandleDataSimulatorServiceImpl implements CandleDataSimulatorService {

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
        String symbol = getRandom().nextBoolean() ? "BTC-USD" : "ETH-USD";
        double base = symbol.equals("BTC-USD") ? 30000 : 2000;
        double price = base + getRandom().nextDouble() * 100;

        BidAskEvent event = new BidAskEvent(
                symbol,
                price - 1,
                price + 1,
                Instant.now().getEpochSecond()
        );

        System.out.println("Generated event: " + event);
        try {
            processEventAsync(event);
        } catch (Exception e) {
            System.err.println("Error processing event: " + e.getMessage());
        }
    }

    @Async
    public void processEventAsync(BidAskEvent event) {
        candleAggregationService.processEvent(event);
    }
}
