package com.trading.candle.aggregator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "candles",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"symbol", "candle_interval", "open_time"}
        )
)
@Getter
@Setter
public class CandleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Column(name = "candle_interval")
    private String candleInterval;

    @Column(name = "open_time")
    private long openTime;

    @Column(name = "open_price")
    private double openPrice;

    @Column(name = "high_price")
    private double highPrice;

    @Column(name = "low_price")
    private double lowPrice;

    @Column(name = "close_price")
    private double closePrice;

    private long volume;
}