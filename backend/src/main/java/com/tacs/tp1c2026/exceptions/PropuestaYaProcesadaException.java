package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando una propuesta ya fue aceptada o rechazada.
 */
public class PropuestaYaProcesadaException extends CustomException {
  public PropuestaYaProcesadaException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}

