package com.trading.candle.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class CandleAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandleAggregatorApplication.class, args);
    }

}
