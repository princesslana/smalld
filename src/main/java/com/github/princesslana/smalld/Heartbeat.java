package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Sends heartbeat payloads to the Discord Gateway. It will begin sending heartbeats after the HELLO
 * payload is received.
 */
public class Heartbeat implements Consumer<SmallD> {

  private final ScheduledThreadPoolExecutor heartbeatExecutor;

  private final SequenceNumber sequenceNumber;

  private ScheduledFuture<?> heartbeat;

  /**
   * Constructs an instance that will send heartbeats.
   *
   * @param sequenceNumber source from which to retrieve last seen sequence number
   */
  public Heartbeat(SequenceNumber sequenceNumber) {
    this.sequenceNumber = sequenceNumber;

    heartbeatExecutor = new ScheduledThreadPoolExecutor(0, SmallD.DAEMON_THREAD_FACTORY);
    heartbeatExecutor.setRemoveOnCancelPolicy(true);
  }

  @Override
  public void accept(SmallD smalld) {
    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          if (p.getInt("op", -1) == 10) {
            onHello(smalld, p.get("d").asObject());
          }
        });
  }

  private void onHello(SmallD smalld, JsonObject d) {
    if (heartbeat != null) {
      heartbeat.cancel(true);
    }

    long interval = d.getInt("heartbeat_interval", -1);

    heartbeat =
        heartbeatExecutor.scheduleAtFixedRate(
            () -> sendHeartbeat(smalld), interval, interval, TimeUnit.MILLISECONDS);
  }

  private void sendHeartbeat(SmallD smalld) {
    smalld.sendGatewayPayload(
        Json.object()
            .add("op", 1)
            .add("d", sequenceNumber.getLastSeen().map(Json::value).orElse(Json.NULL))
            .toString());
  }
}
