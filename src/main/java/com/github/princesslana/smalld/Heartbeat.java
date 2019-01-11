package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Heartbeat {

  private final ScheduledExecutorService heartbeatExecutor =
      Executors.newSingleThreadScheduledExecutor();

  private final SmallD smalld;

  public Heartbeat(SmallD smalld) {

    this.smalld = smalld;

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
    smalld.sendGatewayPayload(Json.object().add("op", 1).add("d", (String) null).toString());
  }
}
