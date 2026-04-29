package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an auction offer is already rejected and an operation expects it to be pending.
 */
public class OfferAlreadyRejectedException extends CustomException {

  public OfferAlreadyRejectedException(String message) {
    super(message, HttpStatus.CONFLICT);
  }

}

