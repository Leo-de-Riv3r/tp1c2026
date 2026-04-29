package com.tacs.tp1c2026.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation is attempted on a closed auction.
 */
public class AuctionClosedException extends CustomException {

  public AuctionClosedException(String message) {
    super(message, HttpStatus.CONFLICT);
  }

}

