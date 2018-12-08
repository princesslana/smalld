package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestIdentify {

  private Identify subject;

  private SmallD smalld;

  private MockDiscordServer server;

  @BeforeEach
  public void subject() {
    server = new MockDiscordServer();
    server.enqueueConnect();

    smalld = server.newSmallD();

    subject = new Identify(smalld);
  }

  @AfterEach
  public void closeSmallD() {
    smalld.close();
  }

  @AfterEach
  public void shutdownServer() {
    server.close();
  }

  private void connect() {
    server.enqueueConnect();
    smalld.connect();
    Assert.thatWithinOneSecond(server::assertConnected);
  }

  @Test
  public void subject_whenHelloReceived_shouldSendIdentify() {
    server.gateway().onOpen((ws, r) -> ws.send(Json.object().add("op", 10).toString()));

    connect();

    Assert.thatWithinOneSecond(
        () -> {
          server.gateway().assertMessage(j -> j.getInt("op", -1)).isEqualTo(2);
        });
  }

  @Test
  public void subject_whenHeartbeatReceived_shouldSendNothing() {
    server.gateway().onOpen((ws, r) -> ws.send(Json.object().add("op", 1).toString()));

    connect();

    Assert.thatNotWithinOneSecond(() -> server.gateway().assertThatNext().isNotNull());
  }
}
