package com.github.princesslana.smalld.test;

public class SentRequest {

  private final String method;
  private final String path;
  private final String payload;

  public SentRequest(String method, String path, String payload) {
    this.method = method;
    this.path = path;
    this.payload = payload;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public String getPayload() {
    return payload;
  }
}
