package com.trading.candle.aggregator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final AtomicBoolean isAggregationRunning = new AtomicBoolean(true);
    private final AtomicBoolean isPersistenceHealthy = new AtomicBoolean(true);
    private volatile long lastCandleProcessed = System.currentTimeMillis();

    public void setAggregationStatus(boolean isRunning) {
        this.isAggregationRunning.set(isRunning);
    }

    public void setPersistenceStatus(boolean isHealthy) {
        this.isPersistenceHealthy.set(isHealthy);
    }

    public void updateLastCandleProcessed() {
        this.lastCandleProcessed = System.currentTimeMillis();
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();

        boolean isHealthy = isAggregationRunning.get() && isPersistenceHealthy.get();
        long timeSinceLastCandle = System.currentTimeMillis() - lastCandleProcessed;

        response.put("status", isHealthy ? "UP" : "DOWN");
        response.put("aggregationRunning", isAggregationRunning.get());
        response.put("persistenceHealthy", isPersistenceHealthy.get());
        response.put("lastCandleProcessed", lastCandleProcessed);
        response.put("timeSinceLastCandleMs", timeSinceLastCandle);
        response.put("dataStatus", timeSinceLastCandle > 60000 ? "STALE" : "ACTIVE");

        return response;
    }
}
