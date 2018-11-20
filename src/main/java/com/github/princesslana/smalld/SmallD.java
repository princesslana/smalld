package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SmallD {

  private final Configuration config;

  private SmallD(Configuration config) {
    this.config = config;
  }

  private void connect() {}

  private String getGatewayUrl() {
    try {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().url(config.getBaseUrl() + "/gateway").build();

      Response response = client.newCall(request).execute();

      return Json.parse(response.body().charStream()).asObject().getString("url", null);
    } catch (IOException e) {
      throw new SmallDException(e);
    }
  }

  public static SmallD create(String token) {
    return create(Configuration.v6(token));
  }

  public static SmallD create(Configuration config) {
    SmallD smallD = new SmallD(config);

    smallD.connect();

    return smallD;
  }
}
