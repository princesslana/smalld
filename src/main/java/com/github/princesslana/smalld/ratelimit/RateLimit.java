package com.github.princesslana.smalld.ratelimit;

import java.time.Clock;
import java.time.Instant;

/**
 * A {@code RateLimit} permits actions to be performed at a certain rate.
 *
 * <p>{@link #acquire()} will either issue a permit by returning or will deny the request for a
 * permit by throwing a {@link RateLimitException}.
 */
public interface RateLimit {

  /**
   * Acquire a permit from this {@code RateLimit}.
   *
   * @throws RateLimitException if there is no permit available
   */
  void acquire();

  /**
   * Creates a {@code RateLimit} that always issues a permit.
   *
   * @return the created RateLimit
   */
  public static RateLimit allowAll() {
    return () -> {};
  }

  /**
   * Creates a {@code RateLimit} that will deny permit requests until the expiry as passed.
   *
   * @param clock the clock to fetch the current {@link Instant} from
   * @param expiry the time until which permit requests will be denied
   * @return the created RateLimit
   */
  public static RateLimit denyUntil(Clock clock, Instant expiry) {
    return () -> {
      if (clock.instant().isBefore(expiry)) {
        throw new RateLimitException(expiry);
      }
    };
  }
}
