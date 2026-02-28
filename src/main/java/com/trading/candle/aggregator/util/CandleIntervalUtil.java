package com.trading.candle.aggregator.util;

public class CandleIntervalUtil {

    public static long toSeconds(String interval) {
        return switch (interval) {
            case "1s" -> 1;
            case "5s" -> 5;
            case "1m" -> 60;
            case "5m" -> 300;
            case "15m" -> 900;
            case "1h" -> 3600;
            case "4h" -> 14400;
            case "1d" -> 86400;
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }

    public static long alignTime(long timestamp, String interval) {
        long seconds = toSeconds(interval);
        return (timestamp / seconds) * seconds;
    }
    
    public static long alignTimeWithDelay(long timestamp, String interval) {
        long seconds = toSeconds(interval);
        long alignedTime = (timestamp / seconds) * seconds;
        
        // Allow for slight delays (up to 10% of interval)
        long delayThreshold = seconds / 10;
        if (timestamp - alignedTime > delayThreshold) {
            // If event is significantly delayed, align to next interval
            return alignedTime + seconds;
        }
        
        return alignedTime;
    }
}
