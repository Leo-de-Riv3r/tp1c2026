package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando no hay suficientes figuritas disponibles para una operación.
 */
public class InsufficientStickerException extends CustomException {
  public InsufficientStickerException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}

