package com.trading.candle.aggregator.exception;

import com.trading.candle.aggregator.dto.ErrorResponse;

public class ValidationException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public ValidationException(ErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
