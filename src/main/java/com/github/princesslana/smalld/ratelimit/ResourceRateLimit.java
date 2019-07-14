package com.github.princesslana.smalld.ratelimit;

import java.time.Clock;
import java.time.Instant;

/**
 * A {@link RateLimit} that allows permits for Discord resources. The rate limiting method is that
 * required for resources endpoints as outlines in the Discord API.
 *
 * <p>The rate limit is based upon a number of remaining requests and an {@link Instant} at which
 * the rate limit resets. If there are more than zero remaning requests, or the reset {@link
 * Instant} has passed then a permit will be allowed. Otherwise it is denied.
 */
public class ResourceRateLimit implements RateLimit {

  private final Clock clock;

  private long remaining;

  private final Instant reset;

  /**
   * Construct a {@code ResourceRateLimit} that will allow {@code remaining} permits before {@code
   * reset}.
   *
   * @param clock a source for the current {@link Instant}
   * @param remaining the number of allowed permits remaining
   * @param reset the {@link Instant} at which this limit resets
   */
  public ResourceRateLimit(Clock clock, long remaining, Instant reset) {
    this.clock = clock;
    this.remaining = remaining;
    this.reset = reset;
  }

  @Override
  public void acquire() {
    if (clock.instant().isBefore(reset) && remaining <= 0) {
      throw new RateLimitException(reset);
    }

    remaining--;
  }
}
