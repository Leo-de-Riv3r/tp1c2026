package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando se intenta operar sobre una subasta que no permite más cambios.
 */
public class SubastaCerradaException extends CustomException {
  public SubastaCerradaException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}

