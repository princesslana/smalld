package com.github.princesslana.smalld.test;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Stores a record HTTP request that was sent via {@link com.github.princesslana.smalld.SmallD}. */
@Getter
@RequiredArgsConstructor
public class SentRequest {

  private final String method;
  private final String path;
  private final String payload;
}
