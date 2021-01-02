package com.github.princesslana.smalld.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.Request;

/**
 * A bucket for {@link RateLimit}s. We can determine the appropriate bucket from the request method
 * and path.
 *
 * <p>A bucket can be either a bucket id, or a (method,route) pair. A route is a http request path
 * but with all ids that are not major parameters ignored, as per Discord's rate limiting
 * specifications.
 *
 * <p>A request path is converted to a route as follows:
 *
 * <ol>
 *   <li>Split the path into segments
 *   <li>For each segment determine if it is a snowflake. This is done by checking if each character
 *       is numeric.
 *   <li>If a sgement is a snowflake remote it, unless the previous segment indicates it is a major
 *       parameter.
 * </ol>
 *
 * <p>For example, <code>/channels/123/messages/789</code> is split into four segments. Both 123 and
 * 789 are identified as snowflakes. 789 will be removed, as the previous segment is "messages",
 * which is not a major parameter. 123 will be kept, as "channels" is a major parameter.
 */
public class RateLimitBucket {

  private static final Set<String> MAJOR_PARAMETERS =
      Stream.of("channels", "guilds", "webhooks").collect(Collectors.toSet());

  private String bucket;

  private RateLimitBucket(String bucket) {
    this.bucket = bucket;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    RateLimitBucket rhs = (RateLimitBucket) obj;

    return Objects.equals(bucket, rhs.bucket);
  }

  @Override
  public int hashCode() {
    return bucket.hashCode();
  }

  @Override
  public String toString() {
    return String.format("RateLimitBucket(%s)", bucket);
  }

  /**
   * Creats a {@code RateLimitBucket} from an id.
   *
   * @param id the id
   * @return the {@code RateLimitBucket} for the given id
   */
  public static RateLimitBucket ofId(String id) {
    return new RateLimitBucket(id);
  }

  /**
   * Creates a {@code RateLimitBucket} from a HTTP method and path.
   *
   * @param method the HTTP request method
   * @param path the HTTP request path
   * @return the {@code RateLimitBucket} for the given method and path
   */
  public static RateLimitBucket from(String method, String path) {
    return new RateLimitBucket(method + " " + toRoute(path));
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

  private static String toRoute(String path) {
    Deque<String> r = new ArrayDeque<>();

    for (String s : path.split("/")) {
      if (isSnowflake(s) && !MAJOR_PARAMETERS.contains(r.peekLast())) {
        r.addLast("{id}");
      } else {
        r.add(s);
      }
    }

    return String.join("/", r);
  }

  private static boolean isSnowflake(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return s.length() > 0;
  }
}
