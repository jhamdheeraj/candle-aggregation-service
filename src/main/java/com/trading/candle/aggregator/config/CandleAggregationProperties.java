package com.trading.candle.aggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "candle.aggregation")
public class CandleAggregationProperties {

    private List<String> intervals = List.of("1s", "1m");
    private long flushRateMs = 1000;
    private Persistence persistence = new Persistence();
    private Processing processing = new Processing();

    public List<String> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<String> intervals) {
        this.intervals = intervals;
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

    public static class Persistence {
        private int batchSize = 50;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;

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
        private double priceCalculationDivisor = 2.0;
        private int maxConcurrentIntervals = 10;
        private long eventTimeoutMs = 5000;

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
}
