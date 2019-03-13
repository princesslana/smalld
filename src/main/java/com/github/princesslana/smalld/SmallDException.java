package com.github.princesslana.smalld;

/**
 * Exception thrown by SmallD. It is used as the base class for SmallD exceptions or to wrap other
 * exceptions that may be thrown.
 *
 * <p>All exceptions thrown by SmallD are unchecked, or rethrown as unchecked.
 */
public class SmallDException extends RuntimeException {

  /** Constructs an instance with the default message. */
  public SmallDException() {
    super();
  }

  /**
   * Constructs an instance with the given message.
   *
   * @param msg the detail message
   */
  public SmallDException(String msg) {
    super(msg);
  }

  /**
   * Constructs an instance with the given cause.
   *
   * @param cause the cause
   */
  public SmallDException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs an instance with the given message and cause.
   *
   * @param msg the detail message
   * @param cause the cause
   */
  public SmallDException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
