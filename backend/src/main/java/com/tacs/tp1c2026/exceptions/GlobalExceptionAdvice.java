package com.tacs.tp1c2026.exceptions;

import com.tacs.tp1c2026.entities.dto.common.ApiError;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionAdvice {

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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {

    Map<String, String> errores = new HashMap<>();

    // Recorremos todos los errores que encontró Spring en el DTO
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String nombreCampo = ((FieldError) error).getField();
      String mensajeError = error.getDefaultMessage();
      errores.put(nombreCampo, mensajeError);
    });

    // Devolvemos un JSON limpio con código 400
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
  }
}
