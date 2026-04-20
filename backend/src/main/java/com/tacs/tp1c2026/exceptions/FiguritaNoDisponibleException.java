package com.tacs.tp1c2026.exceptions;

/**
 * Excepción de dominio lanzada cuando una figurita no está disponible para intercambio u oferta.
 */
public class FiguritaNoDisponibleException extends Exception {
  public FiguritaNoDisponibleException(String message) {
    super(message);
  }
}

