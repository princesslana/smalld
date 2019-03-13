package com.github.princesslana.smalld.examples;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import com.github.princesslana.smalld.SmallD;

/**
 * A bot that will log all payloads received to a channel.
 *
 * <p>Requires two environment variables
 *
 * <ul>
 *   <li><strong>SMALLD_TOKEN</strong> the bot token
 *   <li><strong>CHANNEL_ID</strong> the id of the channel to log payloads
 * </ul>
 */
public class GatewayPayloadLogger {

  /**
   * Entrypoint to run the bot.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    String channel = System.getenv("CHANNEL_ID");

    try (SmallD smalld = SmallD.create(System.getenv("SMALLD_TOKEN"))) {
      smalld.onGatewayPayload(
          p -> {
            JsonObject in = Json.parse(p).asObject();

            if (!isMessageFromBot(in)) {
              String out = in.toString(WriterConfig.PRETTY_PRINT);

              if (out.length() > 1950) {
                out = out.substring(0, 1950) + "...";
              }

              String content = "```javascript\n" + out + "\n```";

              smalld.post(
                  "/channels/" + channel + "/messages",
                  Json.object().add("content", content).toString());
            }
          });

      smalld.run();
    }
  }

  private static boolean isMessageFromBot(JsonObject payload) {
    if (payload.getInt("op", -1) != 0) {
      return false;
    }
    if (!payload.getString("t", "").equals("MESSAGE_CREATE")) {
      return false;
    }
    return payload.get("d").asObject().get("author").asObject().getBoolean("bot", false);
  }
}
