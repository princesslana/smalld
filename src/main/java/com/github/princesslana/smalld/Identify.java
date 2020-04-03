package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Identifies with the Discord Gateway. When a HELLO event is received it will send an IDENTIFY or
 * RESUME payload as necessary.
 */
public class Identify implements Consumer<SmallD> {

  private final SequenceNumber sequenceNumber;

  private Optional<String> sessionId = Optional.empty();

  /**
   * Constructs an instance that will identify and resume as appropriate.
   *
   * @param sequenceNumber source for obtaining the last seen sequence number
   */
  public Identify(SequenceNumber sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  @Override
  public void accept(SmallD smalld) {
    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          int op = p.getInt("op", -1);
          JsonValue t = p.get("t");

          switch (op) {
            case 0:
              if (t != null && t.isString() && t.asString().equals("READY")) {
                onReady(p.get("d").asObject());
              }
              break;

            case 9:
              onInvalidSession(smalld);
              break;

            case 10:
              onHello(smalld);
              break;

            default:
              // do nothing
          }
        });
  }

  private void onHello(SmallD smalld) {
    JsonObject payload =
        sequenceNumber
            .getLastSeen()
            .flatMap(seq -> sessionId.map(sid -> resume(smalld, seq, sid)))
            .orElse(identify(smalld));

    smalld.sendGatewayPayload(payload.toString());
  }

  private JsonObject identify(SmallD smalld) {
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

  private JsonObject resume(SmallD smalld, Long seq, String session) {
    JsonObject d =
        Json.object().add("token", smalld.getToken()).add("session_id", session).add("seq", seq);

    return Json.object().add("op", 6).add("d", d);
  }

  private void onReady(JsonObject d) {
    this.sessionId = Optional.of(d.get("session_id").asString());
  }

  private void onInvalidSession(SmallD smalld) {
    this.sessionId = Optional.empty();

    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    identify(smalld);
  }
}
