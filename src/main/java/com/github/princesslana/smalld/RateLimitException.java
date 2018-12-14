package com.github.princesslana.smalld;

public class RateLimitException extends ClientException {

  public RateLimitException() {
    super();
  }

  public RateLimitException(String msg) {
    super(msg);
  }

  public RateLimitException(Throwable cause) {
    super(cause);
  }

  public RateLimitException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
