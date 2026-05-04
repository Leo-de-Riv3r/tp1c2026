package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

public class UnprocessableException extends CustomException {
  public UnprocessableException(String message) {
    super(message, HttpStatus.UNPROCESSABLE_CONTENT);
  }
}
