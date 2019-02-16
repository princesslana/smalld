package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.Optional;

public class Identify {

  private final SmallD smalld;

  private Optional<Long> lastSeenSequenceNumber = Optional.empty();

  private Optional<String> sessionId = Optional.empty();

  public Identify(SmallD smalld) {
    this.smalld = smalld;

    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          if (p.getInt("op", -1) == 10) {
            onHello();
          }

          if (p.getInt("op", -1) == 0 && p.getString("t", "").equals("READY")) {
            onReady(p.get("d").asObject());
          }

          JsonValue sequence = p.get("s");

          if (sequence != null && !sequence.isNull()) {
            lastSeenSequenceNumber = Optional.of(sequence.asLong());
          }
        });
  }

  private void onHello() {
    JsonObject payload =
        lastSeenSequenceNumber
            .flatMap(seq -> sessionId.map(sid -> resume(seq, sid)))
            .orElse(identify());

    smalld.sendGatewayPayload(payload.toString());
  }

  private JsonObject identify() {
    JsonObject properties =
        Json.object()
            .add("$os", System.getProperty("os.name"))
            .add("$device", "SmallD")
            .add("$browser", "SmallD");

    JsonObject d =
        Json.object()
            .add("token", smalld.getToken())
            .add("properties", properties)
            .add("compress", false)
            .add(
                "shard",
                Json.array().add(smalld.getCurrentShard()).add(smalld.getNumberOfShards()));

    return Json.object().add("op", 2).add("d", d);
  }

  private JsonObject resume(Long seq, String session) {
    JsonObject d =
        Json.object().add("token", smalld.getToken()).add("session_id", session).add("seq", seq);

    return Json.object().add("op", 6).add("d", d);
  }

  private void onReady(JsonObject d) {
    JsonValue sessionId = d.get("session_id");

    if (sessionId != null && !sessionId.isNull()) {
      this.sessionId = Optional.of(sessionId.asString());
    }
  }
}
