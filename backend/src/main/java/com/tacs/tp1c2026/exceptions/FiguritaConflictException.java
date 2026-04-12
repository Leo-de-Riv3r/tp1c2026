package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

public class FiguritaConflictException extends CustomException{
  public FiguritaConflictException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}
