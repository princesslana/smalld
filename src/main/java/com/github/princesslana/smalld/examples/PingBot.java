package com.github.princesslana.smalld.examples;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.princesslana.smalld.SmallD;
import java.util.function.Consumer;

/** A bot that will response to a ping message. */
public class PingBot implements Consumer<SmallD> {

  public void accept(SmallD smalld) {
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
  }

  /**
   * Entrypoint to run bot.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    SmallD.run(System.getenv("SMALLD_TOKEN"), new PingBot());
  }
}
