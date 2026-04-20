package com.tacs.tp1c2026.exceptions;

/**
 * Excepción de dominio lanzada cuando se intenta operar sobre una subasta que no permite más cambios.
 */
public class SubastaCerradaException extends Exception {
  public SubastaCerradaException(String message) {
    super(message);
  }
}

