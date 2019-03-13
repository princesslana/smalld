package com.github.princesslana.smalld;

import java.time.Instant;

/**
 * Exception thrown when a rate limit has been exceeded and a time at which this rate limit will
 * expire is known.
 *
 * <p>When this exception is caught it is possible for the catcher to query {@link #getExpiry} and
 * reschedule a retry for after that {@link Instant}. But it is important to keep in mind that the
 * {@link Instant} returned by {@link #getExpiry()} indicates a time for which we can expect all
 * retries made before that to fail. It does not guarentee that attempts made after that time will
 * not be rate limited again.
 */
public class RateLimitException extends SmallDException {

  private final Instant expiry;

  /**
   * Constructs a {@code RateLimitException} with an expiry {@link Instant}.
   *
   * @param expiry the {@link Instant} at which the rate limit will expire
   */
  public RateLimitException(Instant expiry) {
    this.expiry = expiry;
  }

  /**
   * Returns the {@link Instant} at which the rate limit expires.
   *
   * <p>If the action that was rate limited is retried before this {@link Instant} it is expected
   * that it will fail again. There is no guarentee that a retry after this {@link Instant} will
   * suceed.
   *
   * @return the {@link Instant} at which the rate limit expires
   */
  public Instant getExpiry() {
    return expiry;
  }
}
