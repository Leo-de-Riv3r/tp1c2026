package com.tacs.tp1c2026.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(UsuarioNotFoundException exception) {
        return ResponseEntity.status(404).body(new ErrorResponse("USER_NOT_FOUND", exception.getMessage()));
    }

    public record ErrorResponse(String code, String message){}

}
