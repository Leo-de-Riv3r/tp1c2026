package com.tacs.tp1c2026.exceptions;
/**
 * Excepción de dominio lanzada cuando una oferta ya fue procesada (aceptada o rechazada).
 */
public class OfertaYaProcesadaException extends Exception {
  public OfertaYaProcesadaException(String message) {
    super(message);
  }
}
