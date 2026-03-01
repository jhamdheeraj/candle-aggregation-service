package com.trading.candle.aggregator.config;

import com.trading.candle.aggregator.controller.HealthController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ApplicationLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationLifecycleManager.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    private volatile boolean isShuttingDown = false;
    private final HealthController healthController;

    public ApplicationLifecycleManager(HealthController healthController) {
        this.healthController = healthController;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Candle Aggregation Service is ready");
        healthController.setAggregationStatus(true);
        healthController.setPersistenceStatus(true);
        
        // Add shutdown hook for additional logging
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM Shutdown hook triggered - Candle Aggregation Service stopping");
        }, "candle-shutdown-hook"));
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed(ContextClosedEvent event) {
        logger.info("=== Starting graceful shutdown of Candle Aggregation Service ===");
        isShuttingDown = true;
        
        // Signal health check that we're shutting down
        healthController.setAggregationStatus(false);
        
        try {
            // Wait for in-flight operations to complete
            gracefulShutdown(event.getApplicationContext().getBean(ExecutorService.class));
        } catch (Exception e) {
            logger.error("Error during graceful shutdown", e);
        }
        
        logger.info("=== Candle Aggregation Service shutdown completed ===");
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("@PreDestroy called - ApplicationLifecycleManager cleanup");
    }

    private void gracefulShutdown(ExecutorService executorService) {
        if (executorService != null) {
            logger.info("Shutting down executor service with {} second timeout", SHUTDOWN_TIMEOUT_SECONDS);
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate gracefully within {} seconds, forcing shutdown", 
                            SHUTDOWN_TIMEOUT_SECONDS);
                    executorService.shutdownNow();
                } else {
                    logger.info("Executor service terminated gracefully");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted during shutdown", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isShuttingDown() {
        return isShuttingDown;
    }
}
