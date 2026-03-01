package com.trading.candle.aggregator.controller;

import com.trading.candle.aggregator.dto.ErrorResponse;
import com.trading.candle.aggregator.exception.ValidationException;
import com.trading.candle.aggregator.service.CandleHistoryService;
import com.trading.candle.aggregator.validation.CandleHistoryValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/candle-aggregator")
@Validated
public class CandleHistoryController {

    private final CandleHistoryService candleHistoryService;
    private final CandleHistoryValidator validator;

    public CandleHistoryController(CandleHistoryService candleHistoryService, CandleHistoryValidator validator) {
        this.candleHistoryService = candleHistoryService;
        this.validator = validator;
    }

    @GetMapping(value = "/history")
    public ResponseEntity<Map<String, Object>> getCandleHistory(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam long from,
            @RequestParam long to) {

        ErrorResponse validationError = validator.validateInputs(symbol, interval, from, to);
        if (validationError != null) throw new ValidationException(validationError);

        return ResponseEntity.ok(candleHistoryService.getCandleHistory(symbol.trim(), interval.trim(), from, to));
    }
}
