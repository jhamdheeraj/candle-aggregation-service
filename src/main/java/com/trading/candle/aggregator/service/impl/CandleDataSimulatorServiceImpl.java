package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.config.CandleAggregationProperties;
import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.service.CandleAggregationService;
import com.trading.candle.aggregator.service.CandleDataSimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class CandleDataSimulatorServiceImpl implements CandleDataSimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(CandleDataSimulatorServiceImpl.class);

    private final Random random = new Random();
    private final CandleAggregationService candleAggregationService;
    private final CandleAggregationProperties properties;

    public CandleDataSimulatorServiceImpl(CandleAggregationService candleAggregationService,
                                          CandleAggregationProperties properties) {
        this.candleAggregationService = candleAggregationService;
        this.properties = properties;
    }

    protected Random getRandom() {
        return random;
    }

    @Override
    @Scheduled(fixedRateString = "#{@candleAggregationProperties.simulator.eventGenerationRateMs}")
    public void generateEvent() {
        try {
            List<String> symbols = properties.getSupportedSymbols();
            Map<String, Double> baseValues = properties.getSymbolBaseValues();
            String symbol = symbols.get(getRandom().nextInt(symbols.size()));
            double base = baseValues.getOrDefault(symbol, 100.0);
            double priceVariation = properties.getSimulator().getPriceVariationRange();
            double bidAskSpread = properties.getSimulator().getBidAskSpread();
            double price = base + getRandom().nextDouble() * priceVariation;

            BidAskEvent event = new BidAskEvent(
                    symbol,
                    price - bidAskSpread,
                    price + bidAskSpread,
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
