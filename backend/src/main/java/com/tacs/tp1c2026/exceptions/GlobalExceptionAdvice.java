package com.tacs.tp1c2026.exceptions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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
  public ResponseEntity<Map<String, Object>> handleCustomException(CustomException ex) {

    Map<String, Object> body = Map.of(
        "timestamp", LocalDateTime.now(),
        "message", ex.getMessage()
    );

    // Usas .status() y .body() para construir la respuesta
    return ResponseEntity.status(ex.getHttpStatus()).body(body);
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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpectedError(Exception ex) {
    log.error("Error inesperado: {}", ex.getMessage(), ex);
    Map<String, Object> body = Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Error inesperado"
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
