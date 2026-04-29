package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando se intenta publicar una card que ya está publicada.
 */
public class FiguritaYaPublicadaException extends CustomException {
  public FiguritaYaPublicadaException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}

