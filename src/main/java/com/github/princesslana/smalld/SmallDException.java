package com.github.princesslana.smalld;

public class SmallDException extends RuntimeException {

  public SmallDException() {
    super();
  }

  public SmallDException(String msg) {
    super(msg);
  }

  public SmallDException(Throwable cause) {
    super(cause);
  }

  public SmallDException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
