package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio lanzada cuando un usuario no está autorizado para realizar una operación.
 */
public class UsuarioNoAutorizadoException extends CustomException {
  public UsuarioNoAutorizadoException(String message) {
    super(message, HttpStatus.FORBIDDEN);
  }
}

