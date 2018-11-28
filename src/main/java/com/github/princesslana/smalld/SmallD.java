package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.ParseException;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocketListener;

public class SmallD {

  private static final String V6_BASE_URL = "https://discordapp.com/api/v6";

  private final String token;

  private String baseUrl = V6_BASE_URL;

  private final OkHttpClient client = new OkHttpClient();

  public SmallD(String token) {
    this.token = token;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Connection connect() {
    String gatewayUrl = getGatewayUrl();

    Request request = new Request.Builder().url(gatewayUrl).build();

    client.newWebSocket(request, new WebSocketListener() {});

    return null;
  }

  public void run() {
    // try (Connection c = connect()) {
    //  c.await();
    // }
  }

  private String getGatewayUrl() {
    try {
      Request request =
          new Request.Builder()
              .url(baseUrl + "/gateway/bot")
              .header("Authorization", "Bot " + token)
              .build();

      Response response = client.newCall(request).execute();

      String url = Json.parse(response.body().charStream()).asObject().getString("url", null);

      if (url == null) {
        throw new SmallDException("No URL in /gateway/bot request");
      }

      return url;
    } catch (IOException e) {
      throw new SmallDException(e);
    } catch (ParseException e) {
      throw new SmallDException(e);
    }
  }
}
