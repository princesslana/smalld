package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class Identify {

  private final SmallD smalld;

  public Identify(SmallD smalld) {
    this.smalld = smalld;

    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          if (p.getInt("op", -1) == 10) {
            smalld.sendGatewayPayload(Json.object().add("op", 2).toString());
          }
        });
  }
}
