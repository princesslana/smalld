package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestHeartbeat {

  private Heartbeat subject;

  private SmallD smalld;

  private MockDiscordServer server;

  @BeforeEach
  public void subject() {
    server = new MockDiscordServer();
    server.enqueueConnect();

    smalld = server.newSmallD();

    subject = new Heartbeat(smalld);
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
  public void subject_whenHelloReceived_shouldSendHeartbeat() {
    String hello =
        Json.object()
            .add("op", 10)
            .add("d", Json.object().add("heartbeat_interval", 500))
            .toString();

    server.gateway().onOpen((ws, r) -> ws.send(hello));

    server.connect(smalld);

    Assert.thatWithinOneSecond(() -> server.gateway().assertJsonMessage().node("op").isEqualTo(1));
  }

  @Test
  public void subject_whenDispatchReceived_shouldNotSendHeartbeat() {
    String dispatch =
        Json.object()
            .add("op", 0)
            .add("d", Json.object().add("heartbeat_interval", 500))
            .toString();

    server.gateway().onOpen((ws, r) -> ws.send(dispatch));

    server.connect(smalld);

    Assert.thatNotWithinOneSecond(() -> server.gateway().assertThatNext().isNotNull());
  }
}
