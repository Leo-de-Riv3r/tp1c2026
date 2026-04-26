package com.tacs.tp1c2026.exceptions;

import com.tacs.tp1c2026.entities.dto.common.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionAdvice.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiError> handleCustomException(CustomException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ApiError.of(
                ex.getHttpStatus().value(),
                ex.getHttpStatus().getReasonPhrase(),
                ex.getMessage()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
            .internalServerError()
            .body(ApiError.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}