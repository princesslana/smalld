package com.github.princesslana.smalld;

import java.time.Instant;

public class RateLimitException extends SmallDException {

  private Instant expiry;

  public RateLimitException(Instant expiry) {
    this.expiry = expiry;
  }

  public Instant getExpiry() {
    return expiry;
  }
}
