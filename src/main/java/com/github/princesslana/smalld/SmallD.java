package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.ParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SmallD implements AutoCloseable {

  private static final String V6_BASE_URL = "https://discordapp.com/api/v6";

  private final String token;

  private String baseUrl = V6_BASE_URL;

  private final OkHttpClient client = new OkHttpClient();

  private final List<Consumer<String>> listeners = new ArrayList<>();

  private final CountDownLatch closeGate = new CountDownLatch(1);

  private WebSocket gatewayWebSocket;

  public SmallD(String token) {
    this.token = token;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void connect() {
    String gatewayUrl = getGatewayUrl();

    Request request = new Request.Builder().url(gatewayUrl).build();

    gatewayWebSocket =
        client.newWebSocket(
            request,
            new WebSocketListener() {
              @Override
              public void onMessage(WebSocket ws, String text) {
                listeners.forEach(l -> l.accept(text));
              }
            });
  }

  public void onGatewayPayload(Consumer<String> consumer) {
    listeners.add(consumer);
  }

  public void await() {
    try {
      closeGate.await();
    } catch (InterruptedException e) {
      // ignore
    }
  }

  public void close() {
    if (gatewayWebSocket != null) {
      gatewayWebSocket.close(1000, "Closed.");
    }
    closeGate.countDown();
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
