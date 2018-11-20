package com.github.princesslana.smalld;

public class Configuration {

  private static final String V6_BASE_URL = "https://discordapp.com/api/v6";

  private final String baseUrl;
  private final String token;

  private Configuration(String token, String baseUrl) {
    this.token = token;
    this.baseUrl = baseUrl;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getToken() {
    return token;
  }

  public static Configuration v6(String token) {
    return create(token, V6_BASE_URL);
  }

  public static Configuration create(String token, String baseUrl) {
    return new Configuration(token, baseUrl);
  }
}
