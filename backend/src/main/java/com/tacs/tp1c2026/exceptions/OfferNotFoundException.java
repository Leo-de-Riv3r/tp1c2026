package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an auction offer cannot be found or does not belong to the auction.
 */
public class OfferNotFoundException extends CustomException {

  public OfferNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }

}

