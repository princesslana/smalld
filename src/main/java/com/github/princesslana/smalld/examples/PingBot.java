package com.github.princesslana.smalld.examples;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.princesslana.smalld.SmallD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingBot {

  private static final Logger LOG = LoggerFactory.getLogger(PingBot.class);

  public static void main(String[] args) {
    SmallD smalld = SmallD.create(System.getenv("SMALLD_TOKEN"));

    smalld.onGatewayPayload(
        p -> {
          JsonObject json = Json.parse(p).asObject();

          LOG.debug("JSON received: " + json);

          if (json.getInt("op", -1) == 0
              && json.getString("t", "").equals("MESSAGE_CREATE")
              && json.get("d").asObject().getString("content", "").equals("++ping")) {

            LOG.debug("ping received");
            String channelId = json.get("d").asObject().getString("channel_id", null);

            smalld.post(
                "/channels/" + channelId + "/messages",
                Json.object().add("content", "pong").toString());
          }
        });

    smalld.run();
  }
}
