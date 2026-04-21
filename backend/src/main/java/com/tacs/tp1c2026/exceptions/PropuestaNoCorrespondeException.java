package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando una propuesta no corresponde a la publicación indicada.
 */
public class PropuestaNoCorrespondeException extends CustomException {
  public PropuestaNoCorrespondeException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}

