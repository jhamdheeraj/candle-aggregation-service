package com.trading.candle.aggregator.validation;

import com.trading.candle.aggregator.dto.ErrorResponse;

import java.util.Set;

public class CandleHistoryValidator {

    private static final Set<String> VALID_INTERVALS = Set.of("1s", "5s", "1m", "5m", "15m", "1h", "4h", "1d");

    public static ErrorResponse validateInputs(String symbol, String interval, long from, long to) {
        ErrorResponse error;
        return (error = validateSymbol(symbol)) != null ? error :
               (error = validateInterval(interval)) != null ? error :
               validateTimestampRange(from, to);
    }

    private static ErrorResponse validateSymbol(String symbol) {
        return (symbol == null || symbol.isBlank()) ?
            new ErrorResponse("BAD_REQUEST", "Symbol cannot be null or empty") : null;
    }

    private static ErrorResponse validateInterval(String interval) {
        if (interval == null || interval.isBlank())
            return new ErrorResponse("BAD_REQUEST", "Interval cannot be null or empty");
        return !VALID_INTERVALS.contains(interval.trim()) ?
            new ErrorResponse("BAD_REQUEST", "Invalid interval. Must be one of: 1s, 5s, 1m, 5m, 15m, 1h, 4h, 1d") : null;
    }

    private static ErrorResponse validateTimestampRange(long from, long to) {
        return from >= to ?
            new ErrorResponse("BAD_REQUEST", "From timestamp must be less than to timestamp") :
            (from < 0 || to < 0) ?
            new ErrorResponse("BAD_REQUEST", "Timestamps cannot be negative") : null;
    }
}
