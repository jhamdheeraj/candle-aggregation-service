package com.trading.candle.aggregator.repository;

import com.trading.candle.aggregator.entity.CandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    @Query("SELECT c FROM CandleEntity c WHERE " +
           "c.symbol = :symbol AND c.candleInterval = :interval AND c.openTime IN :openTimes")
    List<CandleEntity> findBySymbolAndCandleIntervalAndOpenTimeIn(
            @Param("symbol") String symbol,
            @Param("interval") String candleInterval,
            @Param("openTimes") List<Long> openTimes
    );
    
    @Modifying
    @Query("UPDATE CandleEntity c SET " +
           "c.highPrice = GREATEST(c.highPrice, :highPrice), " +
           "c.lowPrice = LEAST(c.lowPrice, :lowPrice), " +
           "c.closePrice = :closePrice, " +
           "c.volume = c.volume + :volume " +
           "WHERE c.id = :candleId")
    int updateCandleAggregation(
            @Param("candleId") Long candleId,
            @Param("highPrice") double highPrice,
            @Param("lowPrice") double lowPrice,
            @Param("closePrice") double closePrice,
            @Param("volume") long volume
    );
}