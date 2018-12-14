package com.github.princesslana.smalld;

public class ClientException extends SmallDException {

  public ClientException() {
    super();
  }

  public ClientException(String msg) {
    super(msg);
  }

  public ClientException(Throwable cause) {
    super(cause);
  }

  public ClientException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
