package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an offer has already been processed (accepted or rejected) and an operation expects it to be pending.
 */
public class OfferAlreadyProcessedException extends CustomException {

  public OfferAlreadyProcessedException(String message) {
    super(message, HttpStatus.CONFLICT);
  }

}

