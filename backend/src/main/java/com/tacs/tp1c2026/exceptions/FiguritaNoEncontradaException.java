package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepcion de dominio lanzada cuando no se encuentra una figurita en la coleccion del usuario.
 */
public class FiguritaNoEncontradaException extends CustomException {
  public FiguritaNoEncontradaException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }
}

