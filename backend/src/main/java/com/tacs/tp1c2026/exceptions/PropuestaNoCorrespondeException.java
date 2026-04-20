package com.tacs.tp1c2026.exceptions;

/**
 * Excepción de dominio lanzada cuando una propuesta no corresponde a la publicación indicada.
 */
public class PropuestaNoCorrespondeException extends Exception {
  public PropuestaNoCorrespondeException(String message) {
    super(message);
  }
}

