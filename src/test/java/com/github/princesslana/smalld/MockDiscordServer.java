package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.io.IOException;
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

  public SmallD newSmallD() {
    SmallD smalld = new SmallD(TOKEN);
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

    web.enqueue(new MockResponse().setBody(getGatewayBotResponse));
  }

  public void enqueueWebSocketResponse() {
    web.enqueue(new MockResponse().withWebSocketUpgrade(gateway));
  }

  public RecordedRequest takeRequest(int n) {
    Assertions.assertThat(web.getRequestCount()).isGreaterThanOrEqualTo(n);

    try {
      RecordedRequest r = null;

      for (int i = 0; i < n; i++) {
        r = web.takeRequest();
      }

      return r;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
