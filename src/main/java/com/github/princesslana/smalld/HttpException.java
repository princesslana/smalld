package com.github.princesslana.smalld;

/** Exception thrown on non succesful HTTP requests. */
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

  /**
   * Returns the HTTP status code of the response that triggered this exception.
   *
   * @return the HTTP status code
   */
  public int getCode() {
    return code;
  }

  /**
   * Returns the HTTP status message of the response that triggered this exception.
   *
   * @return the HTTP status message
   */
  public String getStatus() {
    return status;
  }

  /**
   * Returns the HTTP body of the response that triggered this exception.
   *
   * @return the HTTP body
   */
  public String getBody() {
    return body;
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
