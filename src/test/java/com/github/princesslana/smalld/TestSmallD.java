package com.github.princesslana.smalld;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSmallD {

  private MockWebServer server;

  @BeforeEach
  public void createServer() {
    server = new MockWebServer();
  }

  @AfterEach
  public void shutdownServer() throws IOException {
    server.shutdown();
  }

  @Test
  public void connect_shouldSendGetGatewayBotRequest() {
    enqueueGatewayBotResponse();

    SmallD smallD = new SmallD("DUMMY_TOKEN");
    smallD.setBaseUrl(server.url("/api/v6").toString());
    try (Connection c = smallD.connect()) {}

    RecordedRequest req = takeRequest();

    SoftAssertions.assertSoftly(
        s -> {
          s.assertThat(req.getMethod()).isEqualTo("GET");
          s.assertThat(req.getPath()).isEqualTo("/api/v6/gateway/bot");
        });
  }

  @Test
  public void connect_shouldIncudeTokenOnGetGatewayBotRequest() {
    enqueueGatewayBotResponse();

    SmallD smallD = new SmallD("DUMMY_TOKEN");
    smallD.setBaseUrl(server.url("/api/v6").toString());
    try (Connection c = smallD.connect()) {}

    RecordedRequest req = takeRequest();

    Assertions.assertThat(req.getHeader("Authorization")).isEqualTo("Bot DUMMY_TOKEN");
  }

  private void enqueueGatewayBotResponse() {
    String getGatewayBotResponse =
        "{     \"url\": \"wss://gateway.discord.gg/\", "
            + "\"shards\": 9, "
            + "\"session_start_limit\": { "
            + "  \"total\": 1000, "
            + "  \"remaining\": 999, "
            + "  \"reset_after\": 14400000 "
            + "} }";

    server.enqueue(new MockResponse().setBody(getGatewayBotResponse));
  }

  private RecordedRequest takeRequest() {
    Assertions.assertThat(server.getRequestCount()).isEqualTo(1);

    try {
      return server.takeRequest();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
