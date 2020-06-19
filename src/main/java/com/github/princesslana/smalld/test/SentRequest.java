package com.github.princesslana.smalld.test;

/** Stores a record HTTP request that was sent via {@link com.github.princesslana.smalld.SmallD}. */
public class SentRequest {

  private final String method;
  private final String path;
  private final String payload;

  /**
   * Construct a {@code SentRequest} with the given details.
   *
   * @param method the HTTP method
   * @param path the path requested
   * @param payload the payload sent with the request
   */
  public SentRequest(String method, String path, String payload) {
    this.method = method;
    this.path = path;
    this.payload = payload;
  }

  /**
   * Get the HTTP method.
   *
   * @return the HTTP method
   */
  public String getMethod() {
    return method;
  }

  /**
   * Get the request path.
   *
   * @return the request path.
   */
  public String getPath() {
    return path;
  }

  /**
   * Get the payload sent with the request.
   *
   * @return the payload sent with the request
   */
  public String getPayload() {
    return payload;
  }
}
