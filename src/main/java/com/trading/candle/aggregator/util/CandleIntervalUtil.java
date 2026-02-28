package com.trading.candle.aggregator.util;

public class CandleIntervalUtil {

    public static long toSeconds(String interval) {
        return switch (interval) {
            case "1s" -> 1;
            case "5s" -> 5;
            case "1m" -> 60;
            case "15m" -> 900;
            case "1h" -> 3600;
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }

    public static long alignTime(long timestamp, String interval) {
        long seconds = toSeconds(interval);
        return (timestamp / seconds) * seconds;
    }
}
