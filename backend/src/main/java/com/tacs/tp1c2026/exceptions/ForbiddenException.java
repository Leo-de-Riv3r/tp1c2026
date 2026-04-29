package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends CustomException {
  public ForbiddenException(String message) {
    super(message, HttpStatus.FORBIDDEN);
  }
}
