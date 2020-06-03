package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Identifies with the Discord Gateway. When a HELLO event is received it will send an IDENTIFY or
 * RESUME payload as necessary.
 */
public class Identify implements Consumer<SmallD> {

  private final SequenceNumber sequenceNumber;

  private String sessionId;

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
          GatewayPayload p = GatewayPayload.parse(s);

          switch (p.getOp()) {
            case GatewayPayload.OP_DISPATCH:
              if (p.isT("READY")) {
                onReady(p.getD());
              }
              break;

            case GatewayPayload.OP_INVALID_SESSION:
              onInvalidSession(smalld);
              break;

            case GatewayPayload.OP_HELLO:
              onHello(smalld);
              break;

            default:
              // do nothing
          }
        });
  }

  private void onHello(SmallD smalld) {
    Long seq = sequenceNumber.getLastSeen().orElse(null);

    JsonObject payload =
        seq == null || sessionId == null ? identify(smalld) : resume(smalld, seq, sessionId);

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
            .add("intents", smalld.getIntents())
            .add(
                "shard",
                Json.array().add(smalld.getCurrentShard()).add(smalld.getNumberOfShards()));

    return Json.object().add("op", GatewayPayload.OP_IDENTIFY).add("d", d);
  }

  private JsonObject resume(SmallD smalld, Long seq, String session) {
    JsonObject d =
        Json.object().add("token", smalld.getToken()).add("session_id", session).add("seq", seq);

    return Json.object().add("op", GatewayPayload.OP_RESUME).add("d", d);
  }

  private void onReady(JsonObject d) {
    this.sessionId = d.get("session_id").asString();
  }

  private void onInvalidSession(SmallD smalld) {
    this.sessionId = null;

    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    smalld.sendGatewayPayload(identify(smalld).toString());
  }
}
