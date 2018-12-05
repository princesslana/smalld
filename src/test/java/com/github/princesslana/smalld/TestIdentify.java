package com.github.princesslana.smalld;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestIdentify {

  private Identify subject;

  @BeforeEach
  public void subject() {
    subject = new Identify(null);
  }

  @Test
  public void subject_whenHelloReceived_shouldSendIdentify() {}
}
