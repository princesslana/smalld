package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class Identify {

  public Identify(SmallD smalld) {
    smalld.onGatewayPayload(
        s -> {
          JsonObject p = Json.parse(s).asObject();

          if (p.getInt("op", -1) == 10) {
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

            JsonObject identify = Json.object().add("op", 2).add("d", d);
            smalld.sendGatewayPayload(identify.toString());
          }
        });
  }
}
