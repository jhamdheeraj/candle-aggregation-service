package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.service.CandleDataSimulatorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
public class CandleDataSimulatorServiceImpl implements CandleDataSimulatorService {

    private final Random random = new Random();
    private final CandleAggregationService candleAggregationService;

    CandleDataSimulatorServiceImpl(CandleAggregationService candleAggregationService) {
        this.candleAggregationService = candleAggregationService;
    }

    @Override
    @Scheduled(fixedRate = 200)
    public void generateEvent() {
        String symbol = random.nextBoolean() ? "BTC-USD" : "ETH-USD";
        double base = symbol.equals("BTC-USD") ? 30000 : 2000;
        double price = base + random.nextDouble() * 100;

        BidAskEvent event = new BidAskEvent(
                symbol,
                price - 1,
                price + 1,
                Instant.now().getEpochSecond()
        );

        System.out.println("Generated event: " + event);
        candleAggregationService.processEvent(event);
    }
}
