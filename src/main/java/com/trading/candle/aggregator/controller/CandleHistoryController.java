package com.trading.candle.aggregator.controller;

import com.trading.candle.aggregator.service.CandleHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/candle-aggregator")
public class CandleHistoryController {

    private final CandleHistoryService candleHistoryService;

    CandleHistoryController(CandleHistoryService candleHistoryService) {
        this.candleHistoryService = candleHistoryService;
    }

    @GetMapping(value = "/history")
    public ResponseEntity<?> getCandleHistory(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam long from,
            @RequestParam long to) {

        return ResponseEntity.ok(
                candleHistoryService.getCandleHistory(symbol, interval, from, to)
        );
    }
}
