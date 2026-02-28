package com.trading.candle.aggregator.repository;

import com.trading.candle.aggregator.entity.CandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandleRepository extends JpaRepository<CandleEntity, Long> {
    Optional<CandleEntity>
    findBySymbolAndCandleIntervalAndOpenTime(
            String symbol,
            String candleInterval,
            long openTime
    );

    List<CandleEntity> findBySymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
            String symbol,
            String candleInterval,
            long from,
            long to
    );
}