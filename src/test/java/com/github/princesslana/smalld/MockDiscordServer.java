package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockDiscordServer implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(MockDiscordServer.class);

  public static final String TOKEN = "MOCK_TOKEN";

  private final MockWebServer web = new MockWebServer();

  private final WebSocketRecorder gateway = new WebSocketRecorder();

  private int requestsTaken = 0;

  public SmallD newSmallD() {
    return newSmallD(Clock.systemUTC());
  }

  public SmallD newSmallDAtNow() {
    return newSmallDAtMillis(System.currentTimeMillis());
  }

  public SmallD newSmallDAtMillis(long millis) {
    return newSmallD(Clock.fixed(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
  }

  public SmallD newSmallD(Clock clock) {
    SmallD smalld = new SmallD(Config.builder().setToken(TOKEN).setClock(clock).build());
    smalld.setBaseUrl(web.url("/api/v6").toString());
    return smalld;
  }

  public void close() {
    try {
      web.shutdown();
    } catch (IOException e) {
      LOG.warn("Failed to shutdown MockWebServer", e);
    }
  }

  public WebSocketRecorder gateway() {
    return gateway;
  }

  public void enqueue(MockResponse response) {
    web.enqueue(response);
  }

  public void connect(SmallD smalld) {
    enqueueConnect();
    smalld.connect();
    Assert.thatWithinOneSecond(this::assertConnected);

    // Take the gateway bot and web socket requests
    takeRequest();
    takeRequest();
  }

  public void assertConnected() throws InterruptedException {
    gateway.assertOpened();
  }

  public void enqueueConnect() {
    enqueueGatewayBotResponse();
    enqueueWebSocketResponse();
  }

  public void enqueueGatewayBotResponse() {
    String wsUrl = "ws://" + web.getHostName() + ":" + web.getPort() + "/";
    String getGatewayBotResponse = Json.object().add("url", wsUrl).toString();

    enqueue(getGatewayBotResponse);
  }

  public void enqueue(String body) {
    web.enqueue(new MockResponse().setBody(body));
  }

  public void enqueueWebSocketResponse() {
    web.enqueue(new MockResponse().withWebSocketUpgrade(gateway));
  }

  public int getRequestCount() {
    return web.getRequestCount();
  }

  public RecordedRequest takeRequest() {
    requestsTaken++;

    Assertions.assertThat(web.getRequestCount()).isGreaterThanOrEqualTo(requestsTaken);

    try {
      return web.takeRequest();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
