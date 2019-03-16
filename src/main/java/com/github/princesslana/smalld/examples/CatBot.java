package com.github.princesslana.smalld.examples;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.princesslana.smalld.Attachment;
import com.github.princesslana.smalld.SmallD;
import java.io.IOException;
import java.net.URL;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A bot that will respond to {@code ++cat} with a random cat image. Cats are returned from {@link
 * http://aws.random.cat/meow}. The images are sent as attachments to a discord message.
 */
public class CatBot {

  private static final String CAT_URL = "http://aws.random.cat/meow";

  private static final OkHttpClient OK_HTTP = new OkHttpClient();

  private static final Request CAT_REQUEST = new Request.Builder().url(CAT_URL).get().build();

  private static SmallD smalld;

  /**
   * Entrypoint to run bot.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    try (SmallD smalld = SmallD.create(System.getenv("SMALLD_TOKEN"))) {
      smalld.onGatewayPayload(
          p -> {
            JsonObject json = Json.parse(p).asObject();

            if (json.getInt("op", -1) == 0
                && json.getString("t", "").equals("MESSAGE_CREATE")
                && json.get("d").asObject().getString("content", "").equals("++cat")) {

              String channelId = json.get("d").asObject().getString("channel_id", null);

              sendCat(smalld, channelId);
            }
          });

      smalld.run();
    }
  }

  private static void sendCat(SmallD smalld, String channelId) {
    smalld.post(
        "/channels/" + channelId + "/messages",
        "",
        new Attachment("cat.jpg", MediaType.get("image/jpeg"), getCatUrl(smalld)));
  }

  private static URL getCatUrl(SmallD smalld) {
    try {
      Response response = OK_HTTP.newCall(CAT_REQUEST).execute();

      return new URL(Json.parse(response.body().charStream()).asObject().getString("file", null));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
