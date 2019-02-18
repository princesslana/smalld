package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.Optional;

public class SequenceNumber {

  private Optional<Long> lastSeen = Optional.empty();

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

  public Optional<Long> getLastSeen() {
    return lastSeen;
  }
}
