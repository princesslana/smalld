package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends heartbeat payloads to the Discord Gateway. It will begin sending heartbeats after the HELLO
 * payload is received.
 */
public class Heartbeat {

  private final ScheduledExecutorService heartbeatExecutor =
      Executors.newSingleThreadScheduledExecutor(SmallD.DAEMON_THREAD_FACTORY);

  private final SmallD smalld;

  private final SequenceNumber sequenceNumber;

  /**
   * Constructs an instance that will send heartbeats via the provided {@link SmallD}.
   *
   * @param smalld the {@link SmallD} to send heartbeat paylods through
   * @param sequenceNumber source from which to retrieve last seen sequence number
   */
  public Heartbeat(SmallD smalld, SequenceNumber sequenceNumber) {

    this.smalld = smalld;
    this.sequenceNumber = sequenceNumber;

    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          if (p.getInt("op", -1) == 10) {
            onHello(p.get("d").asObject());
          }
        });
  }

  private void onHello(JsonObject d) {
    long interval = d.getInt("heartbeat_interval", -1);

    heartbeatExecutor.scheduleAtFixedRate(
        this::sendHeartbeat, interval, interval, TimeUnit.MILLISECONDS);
  }

  private void sendHeartbeat() {
    smalld.sendGatewayPayload(
        Json.object()
            .add("op", 1)
            .add("d", sequenceNumber.getLastSeen().map(Json::value).orElse(Json.NULL))
            .toString());
  }
}
