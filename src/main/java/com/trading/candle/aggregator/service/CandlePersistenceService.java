package com.trading.candle.aggregator.service;

import com.trading.candle.aggregator.entity.CandleEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CandlePersistenceService {
    CompletableFuture<Void> persistCandles(List<CandleEntity> candles);
}
