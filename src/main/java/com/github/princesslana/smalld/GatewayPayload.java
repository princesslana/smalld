package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.Optional;

/** A Payload as received from the Discord Gateway. */
public class GatewayPayload {

  public static final int OP_DISPATCH = 0;
  public static final int OP_HEARTBEAT = 1;
  public static final int OP_IDENTIFY = 2;
  public static final int OP_RESUME = 6;
  public static final int OP_RECONNECT = 7;
  public static final int OP_INVALID_SESSION = 9;
  public static final int OP_HELLO = 10;
  public static final int OP_HEARTBEAT_ACK = 11;

  private final JsonObject json;

  private GatewayPayload(JsonObject json) {
    this.json = json;
  }

  /**
   * Get the "op" part of this payload.
   *
   * @return the "op" part of this payload
   * @throws IllegalStateException if there is no op
   */
  public int getOp() {
    int op = json.getInt("op", -1);

    if (op < 0) {
      throw new IllegalStateException("No op received in payload");
    }

    return op;
  }

  /**
   * Checks if the "t" part of the payload is equal to a value.
   *
   * @param other the value to compare to
   * @return whether the "t" part is equal to the given value
   */
  public boolean isT(String other) {
    JsonValue t = json.get("t");

    return t != null && t.isString() && t.asString().equals(other);
  }

  /**
   * Get the "d" part of the payload.
   *
   * @return the "d" part of the payload
   */
  public JsonObject getD() {
    return json.get("d").asObject();
  }

  /**
   * Get the "s" part of the payload.
   *
   * @return the "s" part of the payload
   */
  public Optional<Long> getS() {
    JsonValue s = json.get("s");

    return s != null && s.isNumber() ? Optional.of(s.asLong()) : Optional.empty();
  }

  /**
   * Parses a JSON string to create a GatewayPayload.
   *
   * @param s the String to parse
   * @return the parsed payload
   */
  public static GatewayPayload parse(String s) {
    return new GatewayPayload(Json.parse(s).asObject());
  }
}
