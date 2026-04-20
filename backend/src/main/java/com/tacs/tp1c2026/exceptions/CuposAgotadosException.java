package com.tacs.tp1c2026.exceptions;

/**
 * Excepción de dominio lanzada cuando no hay más cupos disponibles para nuevas propuestas.
 */
public class CuposAgotadosException extends Exception {
  public CuposAgotadosException(String message) {
    super(message);
  }
}

