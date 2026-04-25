package com.tacs.tp1c2026.exceptions;

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
  public ResponseEntity<String> handleCustomException(CustomException ex) {
    return new ResponseEntity<String>(ex.getMessage(), ex.getHttpStatus());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleUnexpectedError(Exception ex) {
    log.error("Error inesperado: {}", ex.getMessage(), ex);
    return new ResponseEntity<String>("Error inesperado", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Esta función "atrapa" la excepción que lanza el @Valid
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
