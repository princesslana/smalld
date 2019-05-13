package com.github.princesslana.smalld;

import java.util.Objects;
import okhttp3.Request;

/**
 * A bucket for {@link RateLimit}s. We can determine the appropriate bucket from the request method
 * and path.
 */
public class RateLimitBucket {

  private String bucket;

  private RateLimitBucket(String bucket) {
    this.bucket = bucket;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(RateLimitBucket.class)) {
      return false;
    }
    RateLimitBucket rhs = (RateLimitBucket) obj;

    return Objects.equals(bucket, rhs.bucket);
  }

  @Override
  public int hashCode() {
    return bucket.hashCode();
  }

  /**
   * Creates a {@code RateLimitBucket} from a HTTP method and path.
   *
   * @param method the HTTP method
   * @param path the HTTP request path
   * @return the {@code RateLimitBucket} for the given method and path
   */
  public static RateLimitBucket from(String method, String path) {
    return new RateLimitBucket(path);
  }

  /**
   * Creates a {@code RateLimitBucket} from a {@link Request}.
   *
   * @param request a HTTP request
   * @return the {@code RateLimitBucket} for the given request
   */
  public static RateLimitBucket from(Request request) {
    return from(request.method(), request.url().encodedPath());
  }
}
