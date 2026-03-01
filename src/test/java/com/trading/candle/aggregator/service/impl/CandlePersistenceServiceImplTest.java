package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.repository.CandleRepository;
import com.trading.candle.aggregator.service.CandlePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandlePersistenceServiceImplTest {

    @Mock
    private CandleRepository candleRepository;

    @Mock
    private Executor taskExecutor;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private CandlePersistenceServiceImpl candlePersistenceService;

    private List<CandleEntity> testCandles;
    private static final String SYMBOL = "BTCUSD";
    private static final String INTERVAL = "1m";

    @BeforeEach
    void setUp() {
        testCandles = List.of(
                createCandleEntity(1L, SYMBOL, INTERVAL, 1640995200L, 100.0, 105.0, 95.0, 102.0, 1000L),
                createCandleEntity(2L, SYMBOL, INTERVAL, 1640995260L, 102.0, 108.0, 98.0, 107.0, 1200L),
                createCandleEntity(3L, SYMBOL + "_5m", "5m", 1640995200L, 100.0, 110.0, 95.0, 109.0, 3000L)
        );
    }

    @Test
    void persistCandles_shouldReturnCompletedFuture_whenEmptyList() {
        CompletableFuture<Void> result = candlePersistenceService.persistCandles(List.of());

        assertTrue(result.isDone());
        assertNull(result.join());
        verifyNoInteractions(taskExecutor, applicationContext, candleRepository);
    }

    @Test
    void persistCandlesTransactional_shouldCallBulkPersistence() {
        candlePersistenceService.persistCandlesTransactional(testCandles);

        // Transactional method should complete without exception
        assertDoesNotThrow(() -> candlePersistenceService.persistCandlesTransactional(testCandles));
    }

    @Test
    void persistCandlesBulk_shouldHandleNewCandles_whenNoExistingCandles() {
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(any(), any(), any()))
                .thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", testCandles);

        verify(candleRepository).saveAll(testCandles);
        verify(candleRepository, never()).updateCandleAggregation(any(), anyDouble(), anyDouble(), anyDouble(), anyLong());
    }

    @Test
    void persistCandlesBulk_shouldUpdateExistingCandles_whenCandlesExist() {
        List<Long> openTimes = List.of(1640995200L, 1640995260L);
        List<CandleEntity> existingCandles = List.of(
                createCandleEntity(1L, SYMBOL, INTERVAL, 1640995200L, 99.0, 104.0, 94.0, 101.0, 900L),
                createCandleEntity(2L, SYMBOL, INTERVAL, 1640995260L, 101.0, 107.0, 97.0, 106.0, 1100L)
        );

        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(eq(SYMBOL), eq(INTERVAL), eq(openTimes)))
                .thenReturn(existingCandles);

        ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", testCandles.subList(0, 2));

        verify(candleRepository).updateCandleAggregation(1L, 105.0, 95.0, 102.0, 1000L);
        verify(candleRepository).updateCandleAggregation(2L, 108.0, 98.0, 107.0, 1200L);
        verify(candleRepository, never()).saveAll(any());
    }

    @Test
    void persistCandlesBulk_shouldHandleMixedNewAndExistingCandles() {
        List<Long> openTimes = List.of(1640995200L, 1640995260L);
        List<CandleEntity> existingCandles = List.of(
                createCandleEntity(1L, SYMBOL, INTERVAL, 1640995200L, 99.0, 104.0, 94.0, 101.0, 900L)
        );

        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(eq(SYMBOL), eq(INTERVAL), eq(openTimes)))
                .thenReturn(existingCandles);

        ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", testCandles.subList(0, 2));

        verify(candleRepository).updateCandleAggregation(1L, 105.0, 95.0, 102.0, 1000L);
        verify(candleRepository).saveAll(List.of(testCandles.get(1)));
    }

    @Test
    void persistCandlesBulk_shouldHandleMultipleSymbolsAndIntervals() {
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(any(), any(), any()))
                .thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", testCandles);

        verify(candleRepository, times(2)).findBySymbolAndCandleIntervalAndOpenTimeIn(any(), any(), any());
        verify(candleRepository).saveAll(testCandles);
    }

    @Test
    void persistCandlesBulk_shouldHandleZeroUpdateCount_whenUpdateFails() {
        List<Long> openTimes = List.of(1640995200L);
        List<CandleEntity> existingCandles = List.of(
                createCandleEntity(1L, SYMBOL, INTERVAL, 1640995200L, 99.0, 104.0, 94.0, 101.0, 900L)
        );

        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(eq(SYMBOL), eq(INTERVAL), eq(openTimes)))
                .thenReturn(existingCandles);
        when(candleRepository.updateCandleAggregation(any(), anyDouble(), anyDouble(), anyDouble(), anyLong()))
                .thenReturn(0);

        ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", List.of(testCandles.get(0)));

        verify(candleRepository).updateCandleAggregation(1L, 105.0, 95.0, 102.0, 1000L);
        verify(candleRepository, never()).saveAll(any());
    }

    @Test
    void persistCandlesBulk_shouldGroupBySymbolAndInterval() {
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(any(), any(), any()))
                .thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", testCandles);

        verify(candleRepository).findBySymbolAndCandleIntervalAndOpenTimeIn(
                eq(SYMBOL), eq(INTERVAL), eq(List.of(1640995200L, 1640995260L)));
        verify(candleRepository).findBySymbolAndCandleIntervalAndOpenTimeIn(
                eq(SYMBOL + "_5m"), eq("5m"), eq(List.of(1640995200L)));
    }

    @Test
    void persistCandlesBulk_shouldHandleLargeNumberOfCandles() {
        List<CandleEntity> largeCandleList = Stream.generate(() -> testCandles.get(0))
                .limit(1000)
                .collect(Collectors.toList());

        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(any(), any(), any()))
                .thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", largeCandleList);

        verify(candleRepository).saveAll(largeCandleList);
    }

    @Test
    void persistCandlesBulk_shouldHandleNullPrices() {
        CandleEntity nullPriceCandle = createCandleEntity(1L, SYMBOL, INTERVAL, 1640995200L,
                0.0, 0.0, 0.0, 0.0, 0L);

        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(any(), any(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", List.of(nullPriceCandle)));

        verify(candleRepository).saveAll(List.of(nullPriceCandle));
    }

    @Test
    void persistCandlesBulk_shouldHandleDatabaseException() {
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeIn(any(), any(), any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () ->
                ReflectionTestUtils.invokeMethod(candlePersistenceService, "persistCandlesBulk", testCandles));
    }

    private CandleEntity createCandleEntity(Long id, String symbol, String interval, long openTime,
                                            double openPrice, double highPrice, double lowPrice,
                                            double closePrice, long volume) {
        CandleEntity candle = new CandleEntity();
        candle.setId(id);
        candle.setSymbol(symbol);
        candle.setCandleInterval(interval);
        candle.setOpenTime(openTime);
        candle.setOpenPrice(openPrice);
        candle.setHighPrice(highPrice);
        candle.setLowPrice(lowPrice);
        candle.setClosePrice(closePrice);
        candle.setVolume(volume);
        return candle;
    }
}
