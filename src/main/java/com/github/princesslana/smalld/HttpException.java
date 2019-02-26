package com.github.princesslana.smalld;

public class HttpException extends SmallDException {

  private final int code;
  private final String status;
  private final String body;

  public HttpException(int code, String status, String body) {
    super(String.format("[%s %s] %s", code, status, body));

    this.code = code;
    this.status = status;
    this.body = body;
  }

  public int getCode() {
    return code;
  }

  public String getStatus() {
    return status;
  }

  public String getBody() {
    return body;
  }

  public static class ClientException extends HttpException {
    public ClientException(int code, String status, String body) {
      super(code, status, body);
    }
  }

  public static class ServerException extends HttpException {
    public ServerException(int code, String status, String body) {
      super(code, status, body);
    }
  }
}
