package com.trading.candle.aggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "candle.aggregation")
public class CandleAggregationProperties {

    private List<String> intervals;
    private List<String> supportedSymbols;
    private Map<String, Double> symbolBaseValues;
    private long flushRateMs;
    private Persistence persistence = new Persistence();
    private Processing processing = new Processing();
    private Simulator simulator = new Simulator();

    public List<String> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<String> intervals) {
        this.intervals = intervals;
    }

    public List<String> getSupportedSymbols() {
        return supportedSymbols;
    }

    public void setSupportedSymbols(List<String> supportedSymbols) {
        this.supportedSymbols = supportedSymbols;
    }

    public Map<String, Double> getSymbolBaseValues() {
        return symbolBaseValues;
    }

    public void setSymbolBaseValues(Map<String, Double> symbolBaseValues) {
        this.symbolBaseValues = symbolBaseValues;
    }

    public long getFlushRateMs() {
        return flushRateMs;
    }

    public void setFlushRateMs(long flushRateMs) {
        this.flushRateMs = flushRateMs;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public Processing getProcessing() {
        return processing;
    }

    public void setProcessing(Processing processing) {
        this.processing = processing;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public void setSimulator(Simulator simulator) {
        this.simulator = simulator;
    }

    public static class Persistence {
        private int batchSize;
        private int maxRetries;
        private long retryDelayMs;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getRetryDelayMs() {
            return retryDelayMs;
        }

        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }
    }

    public static class Processing {
        private double priceCalculationDivisor;
        private int maxConcurrentIntervals;
        private long eventTimeoutMs;

        public double getPriceCalculationDivisor() {
            return priceCalculationDivisor;
        }

        public void setPriceCalculationDivisor(double priceCalculationDivisor) {
            this.priceCalculationDivisor = priceCalculationDivisor;
        }

        public int getMaxConcurrentIntervals() {
            return maxConcurrentIntervals;
        }

        public void setMaxConcurrentIntervals(int maxConcurrentIntervals) {
            this.maxConcurrentIntervals = maxConcurrentIntervals;
        }

        public long getEventTimeoutMs() {
            return eventTimeoutMs;
        }

        public void setEventTimeoutMs(long eventTimeoutMs) {
            this.eventTimeoutMs = eventTimeoutMs;
        }
    }

    public static class Simulator {
        private long eventGenerationRateMs;
        private double priceVariationRange;
        private double bidAskSpread;

        public long getEventGenerationRateMs() {
            return eventGenerationRateMs;
        }

        public void setEventGenerationRateMs(long eventGenerationRateMs) {
            this.eventGenerationRateMs = eventGenerationRateMs;
        }

        public double getPriceVariationRange() {
            return priceVariationRange;
        }

        public void setPriceVariationRange(double priceVariationRange) {
            this.priceVariationRange = priceVariationRange;
        }

        public double getBidAskSpread() {
            return bidAskSpread;
        }

        public void setBidAskSpread(double bidAskSpread) {
            this.bidAskSpread = bidAskSpread;
        }
    }
}
