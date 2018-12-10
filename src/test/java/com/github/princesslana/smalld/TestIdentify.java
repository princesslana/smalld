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

  @Test
  public void subject_whenHelloReceived_shouldSendIdentify() {
    server.gateway().onOpen((ws, r) -> ws.send(Json.object().add("op", 10).toString()));

    server.connect(smalld);

    Assert.thatWithinOneSecond(
        () ->
            server
                .gateway()
                .assertJsonMessage()
                .and(
                    j -> j.node("op").isEqualTo(2),
                    j -> j.node("d.token").isEqualTo(MockDiscordServer.TOKEN),
                    j -> j.node("d.compress").isBoolean().isFalse(),
                    j -> j.node("d.properties.$os").isNotNull(),
                    j -> j.node("d.properties.$device").isEqualTo("SmallD"),
                    j -> j.node("d.properties.$browser").isEqualTo("SmallD")));
  }

  @Test
  public void subject_whenHeartbeatReceived_shouldSendNothing() {
    server.gateway().onOpen((ws, r) -> ws.send(Json.object().add("op", 1).toString()));

    server.connect(smalld);

    Assert.thatNotWithinOneSecond(() -> server.gateway().assertThatNext().isNotNull());
  }
}
