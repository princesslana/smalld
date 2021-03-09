package com.github.princesslana.smalld;

import com.github.princesslana.smalld.ratelimit.RateLimitInterceptor;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * {@code HttpClient} that can be used to connect to Discord. Applies rate limiting, attaches the
 * bot token, and sets an appropriate user agent header.
 *
 * <p>A {@link OkHttpClient} instance is created when first needed. A {@link #close()} method is
 * provided to shut down OkHttp's non-daemon threads allowing for a clean shutdown. {@code
 * HttpClient} can be reused after {@link #close()} is called, as a new {@link OkHttpClient}
 * instance will be created.
 */
@Slf4j
public class HttpClient implements AutoCloseable {

  private final Config config;
  private final String userAgent;

  private OkHttpClient client;

  /**
   * Creates an instance using the provided config.
   *
   * @param config SmallD configuration
   */
  public HttpClient(Config config) {
    this.config = config;
    this.userAgent = loadUserAgent();
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

  private synchronized OkHttpClient getClient() {
    if (client == null) {
      client =
          new OkHttpClient.Builder()
              .addInterceptor(new RateLimitInterceptor(config.getClock()))
              .addInterceptor(addHeader("Authorization", () -> "Bot " + config.getToken()))
              .addInterceptor(addHeader("User-Agent", () -> userAgent))
              .build();
    }
    return client;
  }

  /**
   * Creates a {@link WebSocket} with the given request, sending events to the provided {@link
   * WebSocketListener}.
   *
   * @param request Request to create WebSocket
   * @param listener listener to notify of WebSocket events
   * @return the opened WebSocket
   * @see OkHttpClient#newWebSocket(Request, WebSocketListener)
   */
  public WebSocket newWebSocket(Request request, WebSocketListener listener) {
    return getClient().newWebSocket(request, listener);
  }

  /**
   * Sends a request build with the builder to the given path. The path is relative to the base url
   * that is retrieved from the {@link Config} provided when the {@code HttpClient} was initialized.
   *
   * <p>When calling this method you should provide a map of query parameters where the {@code
   * Object} is a {@link java.lang.String} or can be transformed into a {@link java.lang.String}
   * with {@link String#valueOf(Object)}.
   *
   * @param path path to send the request to
   * @param build UnaryOperator to allow building of the request
   * @param parameters the query string parameters
   * @return the body of the HTTP response
   * @throws com.github.princesslana.smalld.ratelimit.RateLimitException if the request was rate
   *     limited
   * @throws HttpException.ClientException if there was a HTTP 4xx response
   * @throws HttpException.ServerException is there was a HTTP 5xx response
   * @throws HttpException for any non 2xx/4xx/5xx ressponse
   */
  public String send(
      String path, UnaryOperator<Request.Builder> build, Map<String, Object> parameters) {

    HttpUrl.Builder urlBuilder =
        HttpUrl.get(config.getBaseUrl())
            .newBuilder()
            .addPathSegments(path.startsWith("/") ? path.substring(1) : path);

    parameters.forEach(
        (string, object) -> urlBuilder.addQueryParameter(string, String.valueOf(object)));

    Request.Builder builder = new Request.Builder().url(urlBuilder.build());

    return send(build.apply(builder).build());
  }

  private String send(Request request) {
    try (Response response = getClient().newCall(request).execute()) {
      int code = response.code();
      String status = response.message();
      String body = response.body().string();

      log.debug("HTTP Response: [{} {}] {}", code, status, body);

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

  @Override
  public synchronized void close() {
    if (client != null) {
      client.dispatcher().executorService().shutdown();
      client.connectionPool().evictAll();
      client = null;
    }
  }

  private static final Interceptor addHeader(String name, Supplier<String> valueSupplier) {
    return c -> c.proceed(c.request().newBuilder().header(name, valueSupplier.get()).build());
  }
}
