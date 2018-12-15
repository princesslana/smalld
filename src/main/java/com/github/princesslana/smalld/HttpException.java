package com.github.princesslana.smalld;

public class HttpException extends SmallDException {

  private final int code;
  private final String message;
  private final String body;

  public HttpException(int code, String message, String body) {
    super(String.format("[%s %s] %s", code, message, body));

    this.code = code;
    this.message = message;
    this.body = body;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public String getBody() {
    return body;
  }

  public static class ClientException extends HttpException {
    public ClientException(int code, String message, String body) {
      super(code, message, body);
    }
  }

  public static class RateLimitException extends ClientException {
    public RateLimitException(int code, String message, String body) {
      super(code, message, body);
    }
  }

  public static class ServerException extends HttpException {
    public ServerException(int code, String message, String body) {
      super(code, message, body);
    }
  }
}
