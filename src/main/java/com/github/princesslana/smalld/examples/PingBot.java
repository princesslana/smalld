package com.github.princesslana.smalld.examples;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.princesslana.smalld.SmallD;

public class PingBot {

  public static void main(String[] args) {
    try (SmallD smalld = SmallD.create(System.getenv("SMALLD_TOKEN"))) {
      smalld.onGatewayPayload(
          p -> {
            JsonObject json = Json.parse(p).asObject();

            if (json.getInt("op", -1) == 0
                && json.getString("t", "").equals("MESSAGE_CREATE")
                && json.get("d").asObject().getString("content", "").equals("++ping")) {

              String channelId = json.get("d").asObject().getString("channel_id", null);

              smalld.post(
                  "/channels/" + channelId + "/messages",
                  Json.object().add("content", "pong").toString());
            }
          });

      smalld.run();
    }
  }
}
