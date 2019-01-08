package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.util.function.BiConsumer;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestIdentify {

  private Identify subject;

  private SmallD smalld;

  private MockDiscordServer server;

  private static final BiConsumer<WebSocket, Response> SEND_HELLO =
      (ws, r) -> ws.send(Json.object().add("op", 10).toString());

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
    server.gateway().onOpen(SEND_HELLO);

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
                    j -> j.node("d.shard").isArray().containsExactly(0, 1),
                    j -> j.node("d.properties.$os").isNotNull(),
                    j -> j.node("d.properties.$device").isEqualTo("SmallD"),
                    j -> j.node("d.properties.$browser").isEqualTo("SmallD")));
  }

  @Test
  public void subject_whenShardSet_shouldSendShardDetail() {
    smalld.setShard(2, 5);

    server.gateway().onOpen(SEND_HELLO);

    server.connect(smalld);

    Assert.thatWithinOneSecond(
        () -> server.gateway().assertJsonMessage().node("d.shard").isArray().containsExactly(2, 5));
  }

  @Test
  public void subject_whenHeartbeatReceived_shouldSendNothing() {
    server.gateway().onOpen((ws, r) -> ws.send(Json.object().add("op", 1).toString()));

    server.connect(smalld);

    Assert.thatNotWithinOneSecond(() -> server.gateway().assertThatNext().isNotNull());
  }
}
