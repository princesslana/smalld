package com.github.princesslana.smalld.ratelimit;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OkHttp {@link Interceptor} that enforces rate limits on HTTP requests. */
public class RateLimitInterceptor implements Interceptor {

  private static final Logger LOG = LoggerFactory.getLogger(RateLimitInterceptor.class);

  private final Clock clock;

  private RateLimit globalRateLimit = RateLimit.allowAll();

  private Map<RateLimitBucket, RateLimitBucket> bucketIds = new ConcurrentHashMap<>();
  private Map<RateLimitBucket, RateLimit> resourceRateLimit = new ConcurrentHashMap<>();

  /**
   * Constructs an instance using the provided source of time.
   *
   * @param clock the clock to fetch the current time from
   */
  public RateLimitInterceptor(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    globalRateLimit.acquire();
    getRateLimitForPath(chain.request()).acquire();

    Response response = chain.proceed(chain.request());

    getRateLimitBucket(response)
        .ifPresent(b -> bucketIds.put(getBucketForPath(chain.request()), b));

    getRateLimit(response).ifPresent(rl -> setRateLimitForPath(chain.request(), rl));

    if (response.code() == 429) {
      getRateLimitExpiry(response)
          .ifPresent(
              expiryAt -> {
                if (isGlobalRateLimit(response)) {
                  globalRateLimit = RateLimit.denyUntil(clock, expiryAt);
                } else {
                  setRateLimitForPath(chain.request(), RateLimit.denyUntil(clock, expiryAt));
                }

                throw new RateLimitException(expiryAt);
              });
    }

    return response;
  }

  private RateLimitBucket getBucketForPath(Request request) {
    RateLimitBucket rl = RateLimitBucket.from(request);
    return bucketIds.getOrDefault(rl, rl);
  }

  private RateLimit getRateLimitForPath(Request request) {
    return resourceRateLimit.getOrDefault(getBucketForPath(request), RateLimit.allowAll());
  }

  private void setRateLimitForPath(Request request, RateLimit rateLimit) {
    RateLimitBucket bucket = getBucketForPath(request);

    LOG.debug(
        "Set Rate Limit: {} {} -> {} -> {}",
        request.method(),
        request.url().encodedPath(),
        bucket,
        rateLimit);

    resourceRateLimit.put(getBucketForPath(request), rateLimit);
  }

  private Optional<Instant> getRateLimitExpiry(Response response) {
    Optional<Instant> reset = getRateLimitReset(response);

    Optional<Instant> responseDate =
        Optional.ofNullable(response.header("Date"))
            .map(DateTimeFormatter.RFC_1123_DATE_TIME::parse)
            .map(Instant::from);

    Optional<Instant> retryAfter =
        getRetryAfter(response).map(responseDate.orElse(clock.instant())::plusMillis);

    return Stream.of(reset, retryAfter).filter(Optional::isPresent).map(Optional::get).findFirst();
  }

  private Optional<Long> getRetryAfter(Response response) {
    return headerAsLong(response, "Retry-After");
  }

  private Optional<Instant> getRateLimitReset(Response response) {
    return headerAsLong(response, "X-RateLimit-Reset").map(Instant::ofEpochMilli);
  }

  private Optional<RateLimitBucket> getRateLimitBucket(Response response) {
    return Optional.ofNullable(response.header("X-RateLimit-Bucket")).map(RateLimitBucket::ofId);
  }

  private boolean isGlobalRateLimit(Response response) {
    return Optional.ofNullable(response.header("X-RateLimit-Global"))
        .map(String::toLowerCase)
        .map(Boolean::valueOf)
        .orElse(false);
  }

  private Optional<RateLimit> getRateLimit(Response response) {
    Optional<Long> remaining = headerAsLong(response, "X-RateLimit-Remaining");
    Optional<Instant> reset = getRateLimitReset(response);

    return remaining.flatMap(rem -> reset.map(res -> new ResourceRateLimit(clock, rem, res)));
  }

  private Optional<Long> headerAsLong(Response response, String header) {
    return Optional.ofNullable(response.header(header))
        .map(Double::parseDouble)
        .map(Double::longValue);
  }
}
