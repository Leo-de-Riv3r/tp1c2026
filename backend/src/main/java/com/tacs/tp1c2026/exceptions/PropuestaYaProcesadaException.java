package com.tacs.tp1c2026.exceptions;

/**
 * Excepción de dominio lanzada cuando una propuesta ya fue aceptada o rechazada.
 */
public class PropuestaYaProcesadaException extends Exception {
  public PropuestaYaProcesadaException(String message) {
    super(message);
  }
}

