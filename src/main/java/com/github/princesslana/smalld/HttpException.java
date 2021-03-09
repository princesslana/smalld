package com.github.princesslana.smalld;

import lombok.Getter;

/** Exception thrown on non succesful HTTP requests. */
@Getter
public class HttpException extends SmallDException {

  private final int code;
  private final String status;
  private final String body;

  /**
   * Constructs an instance with the given code, status, and body.
   *
   * @param code the HTTP status code
   * @param status the HTTP status message
   * @param body the HTTP response body
   */
  public HttpException(int code, String status, String body) {
    super(String.format("[%s %s] %s", code, status, body));

    this.code = code;
    this.status = status;
    this.body = body;
  }

  /** Exception thrown for HTTP 4xx responses. */
  public static class ClientException extends HttpException {

    /**
     * Constructs an instance with the given code, status, and body.
     *
     * @param code the HTTP status code
     * @param status the HTTP status message
     * @param body the HTTP response body
     */
    public ClientException(int code, String status, String body) {
      super(code, status, body);
    }
  }

  /** Exception thrown for HTTP 5xx responses. */
  public static class ServerException extends HttpException {

    /**
     * Constructs an instance with the given code, status, and body.
     *
     * @param code the HTTP status code
     * @param status the HTTP status message
     * @param body the HTTP response body
     */
    public ServerException(int code, String status, String body) {
      super(code, status, body);
    }
  }
}
