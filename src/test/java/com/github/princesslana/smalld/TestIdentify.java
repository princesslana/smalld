package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.util.Optional;
import java.util.function.BiConsumer;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestIdentify {

  private Identify subject;

  private SmallD smalld;

  private MockDiscordServer server;

  private static final BiConsumer<WebSocket, Response> SEND_HEARTBEAT =
      (ws, r) -> ws.send(Json.object().add("op", 1).add("s", Json.NULL).toString());

  private static final BiConsumer<WebSocket, Response> SEND_HELLO =
      (ws, r) -> ws.send(Json.object().add("op", 10).add("t", Json.NULL).toString());

  private static final BiConsumer<WebSocket, Response> SEND_READY =
      (ws, r) ->
          ws.send(
              Json.object()
                  .add("op", 0)
                  .add("s", 1)
                  .add("t", "READY")
                  .add("d", Json.object().add("session_id", "abc123"))
                  .toString());

  @BeforeEach
  public void subject() {
    server = new MockDiscordServer();
    server.enqueueConnect();

    smalld = server.newSmallD();

    subject = new Identify(smalld, new SequenceNumber(smalld));
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
  public void subject_whenHelloReceivedAfterReady_shouldSendResume() {
    server.gateway().onOpen(SEND_READY.andThen(SEND_HELLO));

    server.connect(smalld);

    Assert.thatWithinOneSecond(
        () ->
            server
                .gateway()
                .assertJsonMessage()
                .and(
                    j -> j.node("op").isEqualTo(6),
                    j -> j.node("d.token").isEqualTo(MockDiscordServer.TOKEN),
                    j -> j.node("d.session_id").isEqualTo("abc123"),
                    j -> j.node("d.seq").isEqualTo(1)));
  }

  @Test
  public void subject_whenSequenceReceivedBeforeSessionId_shouldSendIdentify() {
    BiConsumer<WebSocket, Response> sendWithSequence =
        (ws, r) -> ws.send(Json.object().add("op", 0).add("s", 1).toString());

    server.gateway().onOpen(sendWithSequence.andThen(SEND_HELLO));

    server.connect(smalld);

    Assert.thatWithinOneSecond(
        () -> server.gateway().assertJsonMessage().and(j -> j.node("op").isEqualTo(2)));
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
    server.gateway().onOpen(SEND_HEARTBEAT);

    server.connect(smalld);

    Assert.thatNotWithinOneSecond(() -> server.gateway().assertThatNext().isNotNull());
  }

  @Test
  public void subject_whenNonReadyReceived_shouldIgnoreSessionId() {
    BiConsumer<WebSocket, Response> sendNonReadySessionId =
        (ws, r) ->
            ws.send(
                Json.object()
                    .add("op", 0)
                    .add("s", 1)
                    .add("t", "OTHER")
                    .add("d", Json.object().add("session_id", "xyz789"))
                    .toString());

    server.gateway().onOpen(SEND_READY.andThen(sendNonReadySessionId));

    server.connect(smalld);

    Assertions.assertThat(subject).hasFieldOrPropertyWithValue("sessionId", Optional.of("abc123"));
  }
}
