package com.github.princesslana.smalld;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RateLimitInterceptor implements Interceptor {

  private final Clock clock;

  private RateLimit globalRateLimit = RateLimit.allowAll();

  private Map<String, RateLimit> resourceRateLimit = new ConcurrentHashMap();

  public RateLimitInterceptor(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    globalRateLimit.acquire();
    getRateLimitForPath(chain.request()).acquire();

    Response response = chain.proceed(chain.request());

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

  private RateLimit getRateLimitForPath(Request request) {
    return resourceRateLimit.getOrDefault(request.url().encodedPath(), RateLimit.allowAll());
  }

  private void setRateLimitForPath(Request request, RateLimit rateLimit) {
    resourceRateLimit.put(request.url().encodedPath(), rateLimit);
  }

  private Optional<Instant> getRateLimitExpiry(Response response) {
    return getRetryAfter(response).map(clock.instant()::plusMillis);
  }

  private Optional<Long> getRetryAfter(Response response) {
    return Optional.ofNullable(response.header("Retry-After")).map(Long::parseLong);
  }

  private boolean isGlobalRateLimit(Response response) {
    return Optional.ofNullable(response.header("X-RateLimit-Global"))
        .map(String::toLowerCase)
        .map(Boolean::valueOf)
        .orElse(false);
  }
}
