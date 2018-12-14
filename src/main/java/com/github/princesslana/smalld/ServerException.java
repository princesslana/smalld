package com.github.princesslana.smalld;

public class ServerException extends SmallDException {

  public ServerException() {
    super();
  }

  public ServerException(String msg) {
    super(msg);
  }

  public ServerException(Throwable cause) {
    super(cause);
  }

  public ServerException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
