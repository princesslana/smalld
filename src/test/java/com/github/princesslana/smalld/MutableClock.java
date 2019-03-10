package com.github.princesslana.smalld;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {

  private Instant now;

  public MutableClock() {
    this(Instant.now());
  }

  public MutableClock(Instant now) {
    this.now = now;
  }

  public long toEpochMilli() {
    return now.toEpochMilli();
  }

  public void plusMillis(long millis) {
    now = now.plusMillis(millis);
  }

  public void set(Instant now) {
    this.now = now;
  }

  @Override
  public ZoneId getZone() {
    return ZoneId.systemDefault();
  }

  @Override
  public Instant instant() {
    return now;
  }

  @Override
  public Clock withZone(ZoneId zoneId) {
    throw new UnsupportedOperationException();
  }
}
