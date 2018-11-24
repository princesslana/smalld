package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SmallD {

  private static final String V6_BASE_URL = "https://discordapp.com/api/v6";

  private final String token;

  private String baseUrl = V6_BASE_URL;

  public SmallD(String token) {
    this.token = token;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Connection connect() {
    getGatewayUrl();
    return null;
  }

  public void run() {
    try (Connection c = connect()) {
      c.await();
    }
  }

  private String getGatewayUrl() {
    try {
      OkHttpClient client = new OkHttpClient();
      Request request =
          new Request.Builder()
              .url(baseUrl + "/gateway/bot")
              .header("Authorization", "Bot " + token)
              .build();

      Response response = client.newCall(request).execute();

      return Json.parse(response.body().charStream()).asObject().getString("url", null);
    } catch (IOException e) {
      throw new SmallDException(e);
    }
  }
}
