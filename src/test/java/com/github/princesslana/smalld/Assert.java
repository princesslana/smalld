package com.github.princesslana.smalld;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.function.Executable;

public class Assert {

  private Assert() {}

  public static void thatFails(ThrowableAssert.ThrowingCallable t) {
    Assertions.assertThatExceptionOfType(AssertionError.class).isThrownBy(t);
  }

  public static void thatWithinOneSecond(Executable exec) {
    assertTimeoutPreemptively(Duration.ofSeconds(1), exec);
  }

  public static void thatNotWithinOneSecond(Executable exec) {
    thatFails(() -> thatWithinOneSecond(exec));
  }
}
