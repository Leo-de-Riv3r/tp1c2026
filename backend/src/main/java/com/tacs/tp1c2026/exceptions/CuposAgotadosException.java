package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando no hay más cupos disponibles para nuevas propuestas.
 */
public class CuposAgotadosException extends CustomException {
  public CuposAgotadosException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}

