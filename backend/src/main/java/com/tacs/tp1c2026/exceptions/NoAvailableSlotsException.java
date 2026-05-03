package com.tacs.tp1c2026.exceptions;

public class NoAvailableSlotsException extends ConflictException {
  public NoAvailableSlotsException(String message) {
    super(message);
  }
}
