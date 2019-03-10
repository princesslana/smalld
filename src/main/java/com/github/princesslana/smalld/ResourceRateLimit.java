package com.github.princesslana.smalld;

import java.time.Clock;
import java.time.Instant;

public class ResourceRateLimit implements RateLimit {

  private final Clock clock;

  private long remaining;

  private final Instant reset;

  public ResourceRateLimit(Clock clock, long remaining, Instant reset) {
    this.clock = clock;
    this.remaining = remaining;
    this.reset = reset;
  }

  public void acquire() {
    if (clock.instant().isBefore(reset) && remaining <= 0) {
      throw new RateLimitException(reset);
    }

    remaining--;
  }
}
