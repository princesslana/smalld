package com.github.princesslana.smalld;

import java.time.Clock;
import java.time.Instant;

public interface RateLimit {

  void acquire();

  public static RateLimit allowAll() {
    return () -> {};
  }

  public static RateLimit denyUntil(Clock clock, Instant expiry) {
    return () -> {
      if (clock.instant().isBefore(expiry)) {
        throw new RateLimitException(expiry);
      }
    };
  }
}
