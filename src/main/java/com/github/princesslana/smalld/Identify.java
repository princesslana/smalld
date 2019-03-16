package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.Optional;

/**
 * Identifies with the Discord Gateway. When a HELLO event is received it will send an IDENTIFY or
 * RESUME payload as necessary.
 */
public class Identify {

  private final SmallD smalld;

  private final SequenceNumber sequenceNumber;

  private Optional<String> sessionId = Optional.empty();

  /**
   * Constructs an instance that will listen and identify via the provided {@link SmallD}.
   *
   * @param smalld the {@link SmallD} to listen and send payloads through
   * @param sequenceNumber source for obtaining the last seen sequence number
   */
  public Identify(SmallD smalld, SequenceNumber sequenceNumber) {
    this.smalld = smalld;
    this.sequenceNumber = sequenceNumber;

    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          if (p.getInt("op", -1) == 10) {
            onHello();
          }

          JsonValue t = p.get("t");

          if (t != null && t.isString() && t.asString().equals("READY")) {
            onReady(p.get("d").asObject());
          }
        });
  }

  private void onHello() {
    JsonObject payload =
        sequenceNumber
            .getLastSeen()
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
    this.sessionId = Optional.of(d.get("session_id").asString());
  }
}
