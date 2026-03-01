package com.trading.candle.aggregator.service.impl;

import com.trading.candle.aggregator.entity.CandleEntity;
import com.trading.candle.aggregator.repository.CandleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandleHistoryServiceImplTest {

    @Mock
    private CandleRepository candleRepository;

    @InjectMocks
    private CandleHistoryServiceImpl candleHistoryService;

    private List<CandleEntity> mockCandles;
    private static final String SYMBOL = "BTCUSD";
    private static final String INTERVAL = "1m";
    private static final long FROM = 1640995200L; // 2022-01-01 00:00:00
    private static final long TO = 1641081600L;   // 2022-01-02 00:00:00

    @BeforeEach
    void setUp() {
        mockCandles = List.of(
                createCandleEntity(1L, SYMBOL, INTERVAL, FROM, 100.0, 105.0, 95.0, 102.0, 1000L),
                createCandleEntity(2L, SYMBOL, INTERVAL, FROM + 60, 102.0, 108.0, 98.0, 107.0, 1200L),
                createCandleEntity(3L, SYMBOL, INTERVAL, FROM + 120, 107.0, 110.0, 103.0, 109.0, 800L)
        );
    }

    @Test
    void getCandleHistory_shouldReturnFormattedResponse_whenCandlesExist() {
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                eq(SYMBOL), eq(INTERVAL), eq(FROM), eq(TO)))
                .thenReturn(mockCandles);

        Map<String, Object> result = candleHistoryService.getCandleHistory(SYMBOL, INTERVAL, FROM, TO);

        assertNotNull(result);
        assertEquals("ok", result.get("s"));

        List<Long> timestamps = (List<Long>) result.get("t");
        List<Double> opens = (List<Double>) result.get("o");
        List<Double> highs = (List<Double>) result.get("h");
        List<Double> lows = (List<Double>) result.get("l");
        List<Double> closes = (List<Double>) result.get("c");
        List<Long> volumes = (List<Long>) result.get("v");

        assertEquals(3, timestamps.size());
        assertEquals(List.of(FROM, FROM + 60, FROM + 120), timestamps);
        assertEquals(List.of(100.0, 102.0, 107.0), opens);
        assertEquals(List.of(105.0, 108.0, 110.0), highs);
        assertEquals(List.of(95.0, 98.0, 103.0), lows);
        assertEquals(List.of(102.0, 107.0, 109.0), closes);
        assertEquals(List.of(1000L, 1200L, 800L), volumes);
    }

    @Test
    void getCandleHistory_shouldReturnEmptyResponse_whenNoCandlesExist() {
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                eq(SYMBOL), eq(INTERVAL), eq(FROM), eq(TO)))
                .thenReturn(List.of());

        Map<String, Object> result = candleHistoryService.getCandleHistory(SYMBOL, INTERVAL, FROM, TO);

        assertNotNull(result);
        assertEquals("ok", result.get("s"));

        List<Long> timestamps = (List<Long>) result.get("t");
        List<Double> opens = (List<Double>) result.get("o");
        List<Double> highs = (List<Double>) result.get("h");
        List<Double> lows = (List<Double>) result.get("l");
        List<Double> closes = (List<Double>) result.get("c");
        List<Long> volumes = (List<Long>) result.get("v");

        assertTrue(timestamps.isEmpty());
        assertTrue(opens.isEmpty());
        assertTrue(highs.isEmpty());
        assertTrue(lows.isEmpty());
        assertTrue(closes.isEmpty());
        assertTrue(volumes.isEmpty());
    }

    @Test
    void getCandleHistory_shouldHandleSingleCandle() {
        CandleEntity singleCandle = createCandleEntity(1L, SYMBOL, INTERVAL, FROM, 100.0, 105.0, 95.0, 102.0, 1000L);
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                eq(SYMBOL), eq(INTERVAL), eq(FROM), eq(TO)))
                .thenReturn(List.of(singleCandle));

        Map<String, Object> result = candleHistoryService.getCandleHistory(SYMBOL, INTERVAL, FROM, TO);

        assertNotNull(result);
        assertEquals("ok", result.get("s"));

        List<Long> timestamps = (List<Long>) result.get("t");
        assertEquals(1, timestamps.size());
        assertEquals(FROM, timestamps.get(0));

        assertEquals(List.of(100.0), result.get("o"));
        assertEquals(List.of(105.0), result.get("h"));
        assertEquals(List.of(95.0), result.get("l"));
        assertEquals(List.of(102.0), result.get("c"));
        assertEquals(List.of(1000L), result.get("v"));
    }

    @Test
    void getCandleHistory_shouldHandleDifferentSymbolsAndIntervals() {
        String differentSymbol = "ETHUSD";
        String differentInterval = "5m";

        CandleEntity ethCandle = createCandleEntity(1L, differentSymbol, differentInterval, FROM, 2000.0, 2100.0, 1900.0, 2050.0, 500L);
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                eq(differentSymbol), eq(differentInterval), eq(FROM), eq(TO)))
                .thenReturn(List.of(ethCandle));

        Map<String, Object> result = candleHistoryService.getCandleHistory(differentSymbol, differentInterval, FROM, TO);

        assertNotNull(result);
        assertEquals("ok", result.get("s"));
        assertEquals(List.of(FROM), result.get("t"));
        assertEquals(List.of(2000.0), result.get("o"));
        assertEquals(List.of(2100.0), result.get("h"));
        assertEquals(List.of(1900.0), result.get("l"));
        assertEquals(List.of(2050.0), result.get("c"));
        assertEquals(List.of(500L), result.get("v"));
    }

    @Test
    void getCandleHistory_shouldHandleZeroPriceValues() {
        CandleEntity zeroPriceCandle = createCandleEntity(1L, SYMBOL, INTERVAL, FROM, 0.0, 0.0, 0.0, 0.0, 0L);
        when(candleRepository.findBySymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                eq(SYMBOL), eq(INTERVAL), eq(FROM), eq(TO)))
                .thenReturn(List.of(zeroPriceCandle));

        Map<String, Object> result = candleHistoryService.getCandleHistory(SYMBOL, INTERVAL, FROM, TO);

        assertNotNull(result);
        assertEquals("ok", result.get("s"));
        assertEquals(List.of(0.0), result.get("o"));
        assertEquals(List.of(0.0), result.get("h"));
        assertEquals(List.of(0.0), result.get("l"));
        assertEquals(List.of(0.0), result.get("c"));
        assertEquals(List.of(0L), result.get("v"));
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
