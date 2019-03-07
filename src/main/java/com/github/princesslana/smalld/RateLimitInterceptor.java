package com.github.princesslana.smalld;

import java.io.IOException;
import java.time.Clock;
import okhttp3.Interceptor;
import okhttp3.Response;

public class RateLimitInterceptor implements Interceptor {

  private final Clock clock;

  public RateLimitInterceptor(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Response intercept(Interceptor.Chain chain) throws IOException {
    Response response = chain.proceed(chain.request());

    if (response.code() == 429) {
      String retryAfter = response.header("Retry-After");

      if (retryAfter != null) {
        throw new RateLimitException(clock.instant().plusMillis(Long.parseLong(retryAfter)));
      }
    }

    return response;
  }
}
