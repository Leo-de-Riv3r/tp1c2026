package com.tacs.tp1c2026.exceptions;

/**
 * Excepción de dominio lanzada cuando se intenta publicar una figurita que ya está publicada.
 */
public class FiguritaYaPublicadaException extends Exception {
  public FiguritaYaPublicadaException(String message) {
    super(message);
  }
}

