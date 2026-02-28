package com.trading.candle.aggregator.repository;

import com.trading.candle.aggregator.entity.CandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandleRepository extends JpaRepository<CandleEntity, Long> {
}