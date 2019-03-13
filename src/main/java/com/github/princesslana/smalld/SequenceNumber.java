package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.Optional;

/** Tracks the last seen sequence number. */
public class SequenceNumber {

  private Optional<Long> lastSeen = Optional.empty();

  /**
   * Construct an instance that will listen to payloads from the given {@link SmallD}.
   *
   * @param smalld the {@link SmallD} to listen for payloads from.
   */
  public SequenceNumber(SmallD smalld) {
    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          JsonValue sequence = p.get("s");

          if (sequence != null && !sequence.isNull()) {
            lastSeen = Optional.of(sequence.asLong());
          }
        });
  }

  /**
   * Return the last seen sequence number, if there is one.
   *
   * @return the last seen sequence number or {@code empty()} if none
   */
  public Optional<Long> getLastSeen() {
    return lastSeen;
  }
}
