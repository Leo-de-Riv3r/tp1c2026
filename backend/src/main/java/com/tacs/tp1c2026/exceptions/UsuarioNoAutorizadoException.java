package com.tacs.tp1c2026.exceptions;

/**
 * Excepción de dominio lanzada cuando un usuario no está autorizado para realizar una operación.
 */
public class UsuarioNoAutorizadoException extends Exception {
  public UsuarioNoAutorizadoException(String message) {
    super(message);
  }
}

