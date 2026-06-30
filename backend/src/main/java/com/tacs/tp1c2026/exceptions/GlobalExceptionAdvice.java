package com.tacs.tp1c2026.exceptions;

import com.tacs.tp1c2026.entities.dto.common.ApiError;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    /**
     * Spring valida los DTOs anotados con @Valid y lanza esta excepción cuando fallan.
     * Concatena field + message en una única descripción para mantener la forma de ApiError uniforme con el resto de los handlers.
     * La validación principal de UX vive en el FE; este mensaje es defensa en profundidad para clientes que no son el FE (curl/Postman) o bugs del FE.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getAllErrors().stream()
            .map(error -> ((FieldError) error).getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Datos inválidos: " + details
            ));
    }

    /**
     * Se dispara cuando dos transacciones concurrentes intentan modificar el mismo documento y la versión del optimistic lock no coincide.
     * Las anotaciones @Retryable en los services ya agotaron sus reintentos antes de llegar a este punto.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict after retries: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiError.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "El recurso fue modificado por otra operación. Reintentá."
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
            .internalServerError()
            .body(ApiError.of(500, "Internal Server Error", "Ocurrió un error inesperado"));
    }
}
