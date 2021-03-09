package com.github.princesslana.smalld.ratelimit;

import com.github.princesslana.smalld.SmallDException;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Exception thrown when a rate limit has been exceeded and a time at which this rate limit will
 * expire is known.
 *
 * <p>When this exception is caught it is possible for the catcher to query {@link #getExpiry()} and
 * reschedule a retry for after that {@link Instant}. But it is important to keep in mind that the
 * {@link Instant} returned by {@link #getExpiry()} indicates a time for which we can expect all
 * retries made before that to fail. It does not guarantee that attempts made after that time will
 * not be rate limited again.
 */
@Getter
@RequiredArgsConstructor
public class RateLimitException extends SmallDException {
  private final Instant expiry;
}
