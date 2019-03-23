package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.ParseException;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code SmallD} is the entry point for the SmallD API. The methods here can be divided into three
 * categories: Lifecycle, Gateway, and Resources.
 *
 * <p>The Lifecycle methods manage the lifecycle of the connection to Discord. The one that is most
 * likely required is {@link #run()}. {@link #run()} will connect to Discord and block until the
 * connection is closed. If greater control of the connection lifecycle is required the other
 * methods involved in the lifecycle are available to be used as well (e.g., {@link #connect()},
 * {@link #await()}).
 *
 * <p>The Gateway methods allow paylods to be sent to Discord and for execution of listeners upon
 * receiving a payload.
 *
 * <p>The Resource methods allow for sending of requests to Discord's REST API. These are named
 * after the possible HTTP methods (e.g., {@link #get(String)}).
 */
public class SmallD implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(SmallD.class);

  private static final String V6_BASE_URL = "https://discordapp.com/api/v6";
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final Config config;

  private int currentShard = 0;
  private int numberOfShards = 1;

  private String baseUrl = V6_BASE_URL;

  private final String userAgent;

  private final OkHttpClient client;

  private final List<Consumer<String>> gatewayPayloadListeners = new ArrayList<>();

  private final List<Runnable> closeListeners = new ArrayList<>();

  private final CountDownLatch closeGate = new CountDownLatch(1);

  private WebSocket gatewayWebSocket;

  /**
   * Construct a {@code SmallD} instance with the provided config.
   *
   * <p>Note that this does not setup any of the default functionality such as identifying,
   * resuming, etc.
   *
   * @param config the config to use with this instance
   */
  public SmallD(Config config) {
    this.config = config;
    this.userAgent = loadUserAgent();
    this.client = buildHttpClient(config.getClock());
  }

  private String loadUserAgent() {
    try {
      Properties version = new Properties();
      version.load(getClass().getResourceAsStream("version.properties"));
      return String.format(
          "DiscordBot (%s, %s)", version.getProperty("url"), version.getProperty("version"));
    } catch (IOException e) {
      throw new SmallDException(e);
    }
  }

  private OkHttpClient buildHttpClient(Clock clock) {
    return new OkHttpClient.Builder()
        .addInterceptor(new RateLimitInterceptor(clock))
        .addInterceptor(addHeader("Authorization", () -> "Bot " + getToken()))
        .addInterceptor(addHeader("User-Agent", this::getUserAgent))
        .build();
  }

  /**
   * Return the Discord bot token that is in use.
   *
   * @return the bot token in use
   */
  public String getToken() {
    return config.getToken();
  }

  /**
   * Return what is configured as the current shard.
   *
   * @return the configuration for current shard
   */
  public int getCurrentShard() {
    return currentShard;
  }

  /**
   * Return what is configured as the number of shards.
   *
   * @return the number of shards configured
   */
  public int getNumberOfShards() {
    return numberOfShards;
  }

  /**
   * Configure the current shard and number of shards.
   *
   * @param current the current shard
   * @param number the number of shards
   */
  public void setShard(int current, int number) {
    this.currentShard = current;
    this.numberOfShards = number;
  }

  /**
   * Set the base URL to be used for reaching the Discord API. If not set this will default to
   * {@code https://discordapp.com/api/v6}
   *
   * @param baseUrl the base URL to be used to reach the Discord API
   */
  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  private String getUserAgent() {
    return userAgent;
  }

  /** Connect to the Discord gateway. */
  public void connect() {
    String gatewayUrl = getGatewayUrl();

    Request request = new Request.Builder().url(gatewayUrl).build();

    gatewayWebSocket =
        client.newWebSocket(
            request,
            new LoggingWebSocketListener(
                LOG,
                new WebSocketListener() {
                  @Override
                  public void onMessage(WebSocket ws, String text) {
                    gatewayPayloadListeners.forEach(l -> l.accept(text));
                  }
                }));
  }

  /**
   * Add a listener for payloads received from the Discord gateway.
   *
   * @param consumer the listener to be called when a payload is received.
   */
  public void onGatewayPayload(Consumer<String> consumer) {
    gatewayPayloadListeners.add(consumer);
  }

  /**
   * Send a payload to the Discord gateway.
   *
   * @param text the payload to send
   */
  public void sendGatewayPayload(String text) {
    LOG.debug("Gateway Send: {}", text);
    gatewayWebSocket.send(text);
  }

  /**
   * Make a HTTP GET request to a Discord REST endpoint.
   *
   * <p>The path provided should start with {@code /} and will be appended to the base URL that has
   * been configured.
   *
   * @param path the path to make the request to
   * @return the body of the HTTP response
   * @throws RateLimitException if the request was rate limited
   * @throws HttpException.ClientException if there was a HTTP 4xx response
   * @throws HttpException.ServerException is there was a HTTP 5xx response
   * @throws HttpException for any non 2xx/4xx/5xx ressponse
   */
  public String get(String path) {
    LOG.debug("HTTP GET {}", path);

    return sendRequest(path, Request.Builder::get);
  }

  /**
   * Make a HTTP POST request to a Discord REST endpoint. The path provided should start with {@code
   * /} and will be appended to the base URL that has been configured.
   *
   * <p>If no attachments are provided the request will be send with a content type of
   * application/json. If attachments are present the content type will be multipart/form-data and
   * the json payload is included in the part named {@code payload_json}.
   *
   * @param path the path to make the request to
   * @param payload the body to be sent with the request
   * @param attachments attachments for a multipart request
   * @return the body of the HTTP response
   * @throws RateLimitException if the request was rate limited
   * @throws HttpException.ClientException if there was a HTTP 4xx response
   * @throws HttpException.ServerException is there was a HTTP 5xx response
   * @throws HttpException for any non 2xx/4xx/5xx ressponse
   */
  public String post(String path, String payload, Attachment... attachments) {
    LOG.debug("HTTP POST {}: {}", path, payload);

    boolean isMultipart = attachments.length > 0;

    return isMultipart
        ? postMultipart(path, payload, attachments)
        : sendRequest(path, b -> b.post(jsonBody(payload)));
  }

  private String postMultipart(String path, String payload, Attachment... attachments) {
    MultipartBody.Builder builder =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", payload);

    for (Attachment a : attachments) {
      builder.addFormDataPart(
          "file", a.getFilename(), RequestBody.create(a.getMediaType(), a.getBytes()));
    }

    return sendRequest(path, b -> b.post(builder.build()));
  }

  /**
   * Make a HTTP PUT request to a Discord REST endpoint.
   *
   * <p>The request will be send with a content type of application/json. The path provided should
   * start with {@code /} and will be appended to the base URL that has been configured.
   *
   * @param path the path to make the request to
   * @param payload the body to be sent with the request
   * @return the body of the HTTP response
   * @throws RateLimitException if the request was rate limited
   * @throws HttpException.ClientException if there was a HTTP 4xx response
   * @throws HttpException.ServerException is there was a HTTP 5xx response
   * @throws HttpException for any non 2xx/4xx/5xx ressponse
   */
  public String put(String path, String payload) {
    LOG.debug("HTTP PUT {}: {}", path, payload);

    return sendRequest(path, b -> b.put(jsonBody(payload)));
  }

  /**
   * Make a HTTP PATCH request to a Discord REST endpoint.
   *
   * <p>The request will be send with a content type of application/json. The path provided should
   * start with {@code /} and will be appended to the base URL that has been configured.
   *
   * @param path the path to make the request to
   * @param payload the body to be sent with the request
   * @return the body of the HTTP response
   * @throws RateLimitException if the request was rate limited
   * @throws HttpException.ClientException if there was a HTTP 4xx response
   * @throws HttpException.ServerException is there was a HTTP 5xx response
   * @throws HttpException for any non 2xx/4xx/5xx ressponse
   */
  public String patch(String path, String payload) {
    LOG.debug("HTTP PATCH {}: {}", path, payload);

    return sendRequest(path, b -> b.patch(jsonBody(payload)));
  }

  /**
   * Make a HTTP DELETE request to a Discord REST endpoint.
   *
   * <p>The request will be send with a content type of application/json. The path provided should
   * start with {@code /} and will be appended to the base URL that has been configured.
   *
   * @param path the path to make the request to
   * @throws RateLimitException if the request was rate limited
   * @throws HttpException.ClientException if there was a HTTP 4xx response
   * @throws HttpException.ServerException is there was a HTTP 5xx response
   * @throws HttpException for any non 2xx/4xx/5xx ressponse
   */
  public void delete(String path) {
    LOG.debug("HTTP DELETE {}", path);

    sendRequest(path, Request.Builder::delete);
  }

  /** Wait for close. Blocks the current thread until it is. */
  public void await() {
    try {
      closeGate.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Close the connection to Discord. Closes the websocket connection to the gateway and calls all
   * {@link #onClose} listeners.
   */
  public void close() {
    if (gatewayWebSocket != null) {
      gatewayWebSocket.close(1000, "Closed.");
    }

    closeListeners.forEach(Runnable::run);
    closeGate.countDown();
  }

  /**
   * Add a listener to be called when closed. It will be called after the Gateway has been
   * disconnected.
   *
   * @param r a {@link Runnable} to be called on close.
   */
  public void onClose(Runnable r) {
    closeListeners.add(r);
  }

  /** Connect and then await until closed. */
  public void run() {
    connect();
    await();
  }

  private String getGatewayUrl() {
    try {
      String url = Json.parse(get("/gateway/bot")).asObject().getString("url", null);

      if (url == null) {
        throw new SmallDException("No URL in /gateway/bot request");
      }

      return url;
    } catch (ParseException e) {
      throw new SmallDException(e);
    }
  }

  private RequestBody jsonBody(String content) {
    return RequestBody.create(JSON, content);
  }

  private String sendRequest(String path, UnaryOperator<Request.Builder> build) {
    Request.Builder builder = new Request.Builder().url(baseUrl + path);

    return sendRequest(build.apply(builder).build());
  }

  private String sendRequest(Request request) {
    try (Response response = client.newCall(request).execute()) {
      int code = response.code();
      String status = response.message();
      String body = response.body().string();

      LOG.debug("HTTP Response: [{} {}] {}", code, status, body);

      if (response.code() >= 500) {
        throw new HttpException.ServerException(code, status, body);
      } else if (response.code() >= 400) {
        throw new HttpException.ClientException(code, status, body);
      } else if (!response.isSuccessful()) {
        throw new HttpException(code, status, body);
      }

      return body;
    } catch (IOException e) {
      throw new SmallDException(e);
    }
  }

  private static final Interceptor addHeader(String name, Supplier<String> valueSupplier) {
    return c -> c.proceed(c.request().newBuilder().header(name, valueSupplier.get()).build());
  }

  /**
   * Creates an instance that will authenticate using the given token.
   *
   * <p>{@code create} will construct a SmallD instance and add helpers that will handle identifying
   * and heartbeating with the Discord gateway.
   *
   * @param token the token to authenticate with
   * @return the created SmallD instance
   */
  public static SmallD create(String token) {
    Config config = Config.builder().setToken(token).build();

    SmallD smalld = new SmallD(config);

    SequenceNumber seq = new SequenceNumber(smalld);
    new Identify(smalld, seq);
    new Heartbeat(smalld, seq);

    return smalld;
  }
}
