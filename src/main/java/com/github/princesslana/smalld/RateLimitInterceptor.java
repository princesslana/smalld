package com.github.princesslana.smalld;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import okhttp3.Interceptor;
import okhttp3.Response;

public class RateLimitInterceptor implements Interceptor {

  private final Clock clock;

  private Instant globalRateLimitUntil = Instant.ofEpochMilli(0);

  public RateLimitInterceptor(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    if (clock.instant().isBefore(globalRateLimitUntil)) {
      throw new RateLimitException(globalRateLimitUntil);
    }

    Response response = chain.proceed(chain.request());

    if (response.code() == 429) {
      String retryAfter = response.header("Retry-After");

      boolean isGlobal =
          Optional.ofNullable(response.header("X-RateLimit-Global"))
              .map(String::toLowerCase)
              .map(Boolean::valueOf)
              .orElse(false);

      if (retryAfter != null) {
        Instant expiryAt = clock.instant().plusMillis(Long.parseLong(retryAfter));

        if (isGlobal) {
          globalRateLimitUntil = expiryAt;
        }

        throw new RateLimitException(expiryAt);
      }
    }

    return response;
  }
}
