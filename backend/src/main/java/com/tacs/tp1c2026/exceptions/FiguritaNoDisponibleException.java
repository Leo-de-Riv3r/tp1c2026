package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando una card no está disponible para intercambio u oferta.
 */
public class FiguritaNoDisponibleException extends CustomException {
  public FiguritaNoDisponibleException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}

