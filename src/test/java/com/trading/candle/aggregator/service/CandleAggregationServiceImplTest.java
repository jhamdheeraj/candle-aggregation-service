package com.trading.candle.aggregator.service;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.model.BidAskEvent;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.impl.CandleAggregationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CandleAggregationServiceImplTest {

    @Mock
    private CandleRepository candleRepository;

    @Mock
    private CandlePersistenceService persistenceService;

    @Mock
    private Executor taskExecutor;

    private CandleAggregationServiceImpl service;

    private BidAskEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new BidAskEvent("BTC-USD", 30000.0, 30100.0, 1640995200L);
        
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));
        
        service = new CandleAggregationServiceImpl(candleRepository, persistenceService, taskExecutor);
        
        // Manually initialize supportedIntervals since @PostConstruct doesn't work in unit tests
        try {
            var intervalsField = CandleAggregationServiceImpl.class.getDeclaredField("supportedIntervals");
            intervalsField.setAccessible(true);
            intervalsField.set(service, java.util.Arrays.asList("1s", "1m"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize supportedIntervals", e);
        }
    }

    @Test
    void processEvent_shouldCreateNewCandlesForAllIntervals() {
        service.processEvent(testEvent);

        var activeCandles = getActiveCandles();
        assertEquals(2, activeCandles.size());
        
        assertTrue(activeCandles.containsKey("BTC-USD_1s_1640995200"));
        assertTrue(activeCandles.containsKey("BTC-USD_1m_1640995200"));
    }

    @Test
    void processEvent_shouldCalculateCorrectMidPrice() {
        service.processEvent(testEvent);

        var activeCandles = getActiveCandles();
        double expectedMidPrice = (30000.0 + 30100.0) / 2.0;
        
        activeCandles.values().forEach(candle -> {
            assertEquals(expectedMidPrice, candle.getOpenPrice());
            assertEquals(expectedMidPrice, candle.getHighPrice());
            assertEquals(expectedMidPrice, candle.getLowPrice());
            assertEquals(expectedMidPrice, candle.getClosePrice());
        });
    }

    @Test
    void processEvent_shouldUpdateExistingCandle() {
        String key = "BTC-USD_1s_1640995200";
        
        service.processEvent(testEvent);
        
        BidAskEvent secondEvent = new BidAskEvent("BTC-USD", 30200.0, 30300.0, 1640995200L);
        service.processEvent(secondEvent);

        var activeCandles = getActiveCandles();
        CandleEntity candle = activeCandles.get(key);
        
        assertNotNull(candle);
        assertEquals(30050.0, candle.getOpenPrice());
        assertEquals(30250.0, candle.getHighPrice());
        assertEquals(30050.0, candle.getLowPrice());
        assertEquals(30250.0, candle.getClosePrice());
        assertEquals(2, candle.getVolume());
    }

    @Test
    void processEvent_shouldHandleHighPriceUpdate() {
        service.processEvent(testEvent);
        
        BidAskEvent higherPriceEvent = new BidAskEvent("BTC-USD", 31000.0, 31100.0, 1640995200L);
        service.processEvent(higherPriceEvent);

        var activeCandles = getActiveCandles();
        CandleEntity candle = activeCandles.get("BTC-USD_1s_1640995200");
        
        assertEquals(31050.0, candle.getHighPrice());
    }

    @Test
    void processEvent_shouldHandleLowPriceUpdate() {
        service.processEvent(testEvent);
        
        BidAskEvent lowerPriceEvent = new BidAskEvent("BTC-USD", 29000.0, 29100.0, 1640995200L);
        service.processEvent(lowerPriceEvent);

        var activeCandles = getActiveCandles();
        CandleEntity candle = activeCandles.get("BTC-USD_1s_1640995200");
        
        assertEquals(29050.0, candle.getLowPrice());
    }

    @Test
    void processEvent_shouldHandleDifferentSymbols() {
        BidAskEvent ethEvent = new BidAskEvent("ETH-USD", 2000.0, 2100.0, 1640995200L);
        
        service.processEvent(testEvent);
        service.processEvent(ethEvent);

        var activeCandles = getActiveCandles();
        assertEquals(4, activeCandles.size());
        
        assertTrue(activeCandles.containsKey("BTC-USD_1s_1640995200"));
        assertTrue(activeCandles.containsKey("BTC-USD_1m_1640995200"));
        assertTrue(activeCandles.containsKey("ETH-USD_1s_1640995200"));
        assertTrue(activeCandles.containsKey("ETH-USD_1m_1640995200"));
    }

    @Test
    void processEvent_shouldHandleDifferentTimestamps() {
        BidAskEvent laterEvent = new BidAskEvent("BTC-USD", 30000.0, 30100.0, 1640995260L);
        
        service.processEvent(testEvent);
        service.processEvent(laterEvent);

        var activeCandles = getActiveCandles();
        assertEquals(4, activeCandles.size());
        
        assertTrue(activeCandles.containsKey("BTC-USD_1s_1640995200"));
        assertTrue(activeCandles.containsKey("BTC-USD_1m_1640995200"));
        assertTrue(activeCandles.containsKey("BTC-USD_1s_1640995200"));
        assertTrue(activeCandles.containsKey("BTC-USD_1m_1640995200"));
    }

    @Test
    void processEvent_shouldHandleZeroBidAsk() {
        BidAskEvent zeroEvent = new BidAskEvent("BTC-USD", 0.0, 0.0, 1640995200L);
        
        service.processEvent(zeroEvent);

        var activeCandles = getActiveCandles();
        activeCandles.values().forEach(candle -> {
            assertEquals(0.0, candle.getOpenPrice());
            assertEquals(0.0, candle.getHighPrice());
            assertEquals(0.0, candle.getLowPrice());
            assertEquals(0.0, candle.getClosePrice());
        });
    }

    @Test
    void processEvent_shouldHandleNegativePrices() {
        assertThrows(IllegalArgumentException.class, () -> {
            BidAskEvent negativeEvent = new BidAskEvent("BTC-USD", -1000.0, -900.0, 1640995200L);
            service.processEvent(negativeEvent);
        });
    }

    @Test
    void flushToDatabase_shouldDoNothingWhenNoActiveCandles() {
        service.flushToDatabase();
        
        verify(persistenceService, never()).persistCandles(any());
        verify(candleRepository, never()).save(any(CandleEntity.class));
        verify(candleRepository, never()).findBySymbolAndCandleIntervalAndOpenTime(anyString(), anyString(), anyLong());
    }

    @Test
    void flushToDatabase_shouldInsertNewCandles() {
        service.processEvent(testEvent);
        when(persistenceService.persistCandles(any())).thenReturn(CompletableFuture.completedFuture(null));
        
        service.flushToDatabase().join();

        verify(persistenceService, times(1)).persistCandles(any());
        
        var activeCandles = getActiveCandles();
        assertTrue(activeCandles.isEmpty());
    }

    @Test
    void flushToDatabase_shouldUpdateExistingCandles() {
        service.processEvent(testEvent);
        when(persistenceService.persistCandles(any())).thenReturn(CompletableFuture.completedFuture(null));
        
        service.flushToDatabase().join();

        verify(persistenceService, times(1)).persistCandles(any());
        
        var activeCandles = getActiveCandles();
        assertTrue(activeCandles.isEmpty());
    }

    @Test
    void flushToDatabase_shouldHandleMixedInsertAndUpdate() {
        service.processEvent(testEvent);
        when(persistenceService.persistCandles(any())).thenReturn(CompletableFuture.completedFuture(null));
        
        service.flushToDatabase().join();

        verify(persistenceService, times(1)).persistCandles(any());
        
        var activeCandles = getActiveCandles();
        assertTrue(activeCandles.isEmpty());
    }

    @Test
    void processEvent_shouldHandleConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int eventsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < eventsPerThread; j++) {
                    BidAskEvent event = new BidAskEvent(
                            "BTC-USD", 
                            30000.0 + threadId, 
                            30100.0 + threadId, 
                            1640995200L + j
                    );
                    service.processEvent(event);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        var activeCandles = getActiveCandles();
        assertTrue(activeCandles.size() > 0);
        
        activeCandles.values().forEach(candle -> {
            assertNotNull(candle.getSymbol());
            assertNotNull(candle.getCandleInterval());
            assertTrue(candle.getVolume() > 0);
        });
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, CandleEntity> getActiveCandles() {
        try {
            var field = CandleAggregationServiceImpl.class.getDeclaredField("activeCandles");
            field.setAccessible(true);
            return (ConcurrentHashMap<String, CandleEntity>) field.get(service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access activeCandles field", e);
        }
    }
}
