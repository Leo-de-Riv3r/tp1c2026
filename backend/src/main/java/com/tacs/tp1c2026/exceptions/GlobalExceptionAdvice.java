package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionAdvice {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<String> handleCustomException(CustomException ex) {
    return new ResponseEntity<String>(ex.getMessage(), ex.getHttpStatus());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleUnexpectedError(Exception ex) {
    return new ResponseEntity<String>("Error inesperado", HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
