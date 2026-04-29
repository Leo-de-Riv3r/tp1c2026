package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando una oferta ya fue procesada (aceptada o rechazada).
 */
public class OfertaYaProcesadaException extends CustomException {
  public OfertaYaProcesadaException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}
