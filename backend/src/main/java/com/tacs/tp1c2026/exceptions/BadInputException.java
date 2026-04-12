package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

public class BadInputException extends CustomException {
  public BadInputException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
