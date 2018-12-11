package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.ParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SmallD implements AutoCloseable {

  private static final String V6_BASE_URL = "https://discordapp.com/api/v6";

  private final String token;

  private String baseUrl = V6_BASE_URL;

  private final OkHttpClient client =
      new OkHttpClient.Builder()
          .addInterceptor(addHeader("Authorization", () -> "Bot " + getToken()))
          .build();

  private final List<Consumer<String>> listeners = new ArrayList<>();

  private final CountDownLatch closeGate = new CountDownLatch(1);

  private WebSocket gatewayWebSocket;

  public SmallD(String token) {
    this.token = token;
  }

  public String getToken() {
    return token;
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

  public void sendGatewayPayload(String text) {
    gatewayWebSocket.send(text);
  }

  public String get(String path) {
    return sendRequest(new Request.Builder().url(baseUrl + path).build());
  }

  public String post(String path, String payload) {
    Request request =
        new Request.Builder()
            .url(baseUrl + path)
            .post(RequestBody.create(MediaType.get("application/json"), payload))
            .build();

    return sendRequest(request);
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
    connect();
    await();
  }

  private String getGatewayUrl() {
    try {
      String url = Json.parse(get("/gateway/bot")).asObject().getString("url", null);

      if (url == null) {
        throw new SmallDException("No URL in /gateway/bot request");
      }

      return url;
    } catch (ParseException e) {
      throw new SmallDException(e);
    }
  }

  private String sendRequest(Request request) {
    try (Response response = client.newCall(request).execute()) {
      return response.body().string();
    } catch (IOException e) {
      throw new SmallDException(e);
    }
  }

  private static final Interceptor addHeader(String name, Supplier<String> valueSupplier) {
    return c -> c.proceed(c.request().newBuilder().header(name, valueSupplier.get()).build());
  }
}
