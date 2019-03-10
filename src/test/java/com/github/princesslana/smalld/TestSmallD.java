package com.github.princesslana.smalld;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSmallD {

  private static final Logger LOG = LoggerFactory.getLogger(TestSmallD.class);

  private SmallD subject;

  private MockDiscordServer server;

  @BeforeEach
  public void subject() {
    server = new MockDiscordServer();
    subject = server.newSmallD();
  }

  @AfterEach
  public void closeSmallD() {
    subject.close();
  }

  @AfterEach
  public void closeServer() {
    server.close();
  }

  @Test
  public void baseUrl_whenNotSetExplicitly_shouldBeDiscordUrl() {
    SmallD defaultSmallD = new SmallD(MockDiscordServer.TOKEN);
    Assertions.assertThat(defaultSmallD)
        .hasFieldOrPropertyWithValue("baseUrl", "https://discordapp.com/api/v6");
  }

  @Test
  public void currentShard_whenNotSetExplicitly_shouldBeZero() {
    SmallD defaultSmallD = new SmallD(MockDiscordServer.TOKEN);
    Assertions.assertThat(defaultSmallD.getCurrentShard()).isEqualTo(0);
  }

  @Test
  public void numberOfShards_whenNotSetExplicitly_shouldBeOne() {
    SmallD defaultSmallD = new SmallD(MockDiscordServer.TOKEN);
    Assertions.assertThat(defaultSmallD.getNumberOfShards()).isEqualTo(1);
  }

  @Test
  public void connect_shouldSendGetGatewayBotRequest() {
    server.enqueueGatewayBotResponse();

    subject.connect();

    RecordedRequest req = server.takeRequest();

    SoftAssertions.assertSoftly(
        s -> {
          s.assertThat(req.getMethod()).isEqualTo("GET");
          s.assertThat(req.getPath()).isEqualTo("/api/v6/gateway/bot");
        });
  }

  @Test
  public void connect_shouldIncudeTokenOnGetGatewayBotRequest() {
    server.enqueueGatewayBotResponse();

    subject.connect();

    RecordedRequest req = server.takeRequest();

    Assertions.assertThat(req.getHeader("Authorization"))
        .isEqualTo("Bot " + MockDiscordServer.TOKEN);
  }

  @Test
  public void connect_whenBadJsonInGetGatewayBotResponse_shouldThrowException() {
    server.enqueue(new MockResponse().setBody("abc"));
    Assertions.assertThatExceptionOfType(SmallDException.class).isThrownBy(subject::connect);
  }

  @Test
  public void connect_whenNoUrlInGetGatewayBotResponse_shouldThrowException() {
    server.enqueue(new MockResponse().setBody("{}"));
    Assertions.assertThatExceptionOfType(SmallDException.class).isThrownBy(subject::connect);
  }

  @Test
  public void connect_when500_shouldThrowException() {
    server.enqueue(new MockResponse().setResponseCode(500));
    Assertions.assertThatExceptionOfType(SmallDException.class).isThrownBy(subject::connect);
  }

  @Test
  public void connect_whenNoResponse_shouldThrowException() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
    Assertions.assertThatExceptionOfType(SmallDException.class).isThrownBy(subject::connect);
  }

  @Test
  public void connect_shouldOpenWebSocketToGatewayBotUrl() {
    server.enqueueConnect();

    subject.connect();

    Assert.thatWithinOneSecond(() -> server.gateway().assertOpened());
  }

  @Test
  public void onGatewayPayload_shouldForwardPayload() {
    String expected = "TEST MESSAGE";
    CompletableFuture<String> received = new CompletableFuture<>();

    server.enqueueConnect();

    server.gateway().onOpen((ws, r) -> ws.send(expected));

    subject.onGatewayPayload(received::complete);
    subject.connect();

    Assert.thatWithinOneSecond(() -> Assertions.assertThat(received.get()).isEqualTo(expected));
  }

  @Test
  public void sendGatewayPayload_shouldSendPayload() {
    String payload = "TEST PAYLOAD";

    server.connect(subject);

    subject.sendGatewayPayload(payload);
    Assert.thatWithinOneSecond(() -> server.gateway().assertMessage(payload));
  }

  @Test
  public void get_shouldMakeGetRequest() {
    server.connect(subject);

    String p = "/test/path";

    server.enqueue("");

    subject.get(p);

    RecordedRequest req = server.takeRequest();

    SoftAssertions.assertSoftly(
        s -> {
          s.assertThat(req.getMethod()).isEqualTo("GET");
          s.assertThat(req.getPath()).isEqualTo("/api/v6" + p);
        });
  }

  @Test
  public void get_shouldIncludeUserAgent() {
    server.connect(subject);

    server.enqueue("");

    subject.get("/test/url");

    Assertions.assertThat(server.takeRequest().getHeader("User-Agent"))
        .matches("DiscordBot (\\S+, \\S+)")
        .doesNotContain("null");
  }

  @ParameterizedTest
  @ValueSource(ints = {300, 301, 302, 399})
  public void get_whenHttp3xx_shouldThrowHttpException(int status) {
    server.connect(subject);

    server.enqueue(new MockResponse().setResponseCode(status));

    Assertions.assertThatExceptionOfType(HttpException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 404, 422, 499})
  public void get_whenHttp4xx_shouldThrowClientException(int status) {
    server.connect(subject);

    server.enqueue(new MockResponse().setResponseCode(status));

    Assertions.assertThatExceptionOfType(HttpException.ClientException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @ParameterizedTest
  @ValueSource(ints = {500, 502, 503, 599})
  public void get_whenHttp5xx_shouldThrowServerException(int status) {
    server.connect(subject);

    server.enqueue(new MockResponse().setResponseCode(status));

    Assertions.assertThatExceptionOfType(HttpException.ServerException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @Test
  public void get_whenHttp429WithNoRetryAfterHeader_shouldThrowClientException() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(429));

    Assertions.assertThatExceptionOfType(HttpException.ClientException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 100})
  public void get_whenHttp429WithRetryAfterHeader_shouldThrowRateLimitException(int retryAfter) {
    long now = System.currentTimeMillis();

    SmallD smalld = server.newSmallDAtMillis(now);

    server.connect(smalld);

    server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", retryAfter));

    Assertions.assertThatThrownBy(() -> smalld.get("/test/url"))
        .isInstanceOf(RateLimitException.class)
        .hasFieldOrPropertyWithValue("expiry", Instant.ofEpochMilli(now + retryAfter));
  }

  @Test
  public void get_whenGlobalRateLimited_shouldNotMakeHttpRequest() {
    SmallD smalld = server.newSmallDAtNow();

    server.connect(smalld);

    server.enqueue(
        new MockResponse()
            .setResponseCode(429)
            .setHeader("Retry-After", 1)
            .setHeader("X-RateLimit-Global", "true"));

    // Make single request to trigger rate limiting
    Assertions.catchThrowable(() -> smalld.get("/test/url1"));

    assertThatNoServerRequest(
        () -> {
          Assertions.assertThatThrownBy(() -> smalld.get("/test/url2"))
              .isInstanceOf(RateLimitException.class);
        });
  }

  @Test
  public void get_whenRateLimitedButNotGlobal_shouldMakeHttpRequestForDifferentUrl() {
    SmallD smalld = server.newSmallDAtNow();

    server.connect(smalld);

    server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", 1));

    server.enqueue(new MockResponse().setResponseCode(200).setBody("response_body"));

    // Make single request to trigger rate limiting
    Assertions.catchThrowable(() -> smalld.get("/test/url1"));
    server.takeRequest();

    int beforeCount = server.getRequestCount();

    String response = smalld.get("/test/url2");

    Assertions.assertThat(response).isEqualTo("response_body");
    Assertions.assertThat(server.getRequestCount()).isEqualTo(beforeCount + 1);
  }

  @Test
  public void get_whenRateLimited_shouldNotMakeHttpRequest() {
    SmallD smalld = server.newSmallDAtNow();

    server.connect(smalld);

    server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", 1));

    // Make single request to trigger rate limiting
    Assertions.catchThrowable(() -> smalld.get("/test/url1"));

    assertThatNoServerRequest(
        () -> {
          Assertions.assertThatThrownBy(() -> smalld.get("/test/url1"))
              .isInstanceOf(RateLimitException.class);
        });
  }

  @Test
  public void get_whenHttp500_shouldIncludeCode() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(500));

    Assertions.assertThatThrownBy(() -> subject.get("test/url"))
        .hasFieldOrPropertyWithValue("code", 500);
  }

  @Test
  public void get_whenHttp500_shouldIncludeStatus() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(500));

    Assertions.assertThatThrownBy(() -> subject.get("test/url"))
        .hasFieldOrPropertyWithValue("status", "Server Error");
  }

  @Test
  public void get_whenHttp500_shouldIncludeBody() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(500).setBody("TEST BODY"));

    Assertions.assertThatThrownBy(() -> subject.get("test/url"))
        .hasFieldOrPropertyWithValue("body", "TEST BODY");
  }

  @Test
  public void post_shouldPostPayloadToEndpoint() {
    String payload = "TEST PAYLOAD";

    server.connect(subject);

    server.enqueue("");

    subject.post("/test/url", payload);

    RecordedRequest req = server.takeRequest();

    SoftAssertions.assertSoftly(
        s -> {
          s.assertThat(req.getMethod()).isEqualTo("POST");
          s.assertThat(req.getPath()).isEqualTo("/api/v6/test/url");
          s.assertThat(req.getBody().readUtf8()).isEqualTo(payload);
        });
  }

  @Test
  public void post_shouldReturnResponseOn200() {
    String expected = "TEST RESPONSE";

    server.connect(subject);

    server.enqueue(expected);

    String actual = subject.post("/test/url", "");

    Assertions.assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void post_shouldIncludeToken() {
    server.connect(subject);

    server.enqueue("");

    subject.post("/test/url", "");

    RecordedRequest req = server.takeRequest();

    Assertions.assertThat(req.getHeader("Authorization"))
        .isEqualTo("Bot " + MockDiscordServer.TOKEN);
  }

  @Test
  public void await_whenClose_shouldComplete() {
    Executors.newSingleThreadScheduledExecutor()
        .schedule(subject::close, 200, TimeUnit.MILLISECONDS);
    Assert.thatWithinOneSecond(subject::await);
  }

  @Test
  public void await_whenNoClose_shouldNotComplete() {
    Assert.thatNotWithinOneSecond(subject::await);
  }

  @Test
  public void await_whenInterrupted_shouldReinterrupt() {
    Assert.thatWithinOneSecond(
        () -> {
          Thread.currentThread().interrupt();
          subject.await();
          Assertions.assertThat(Thread.currentThread().isInterrupted()).isTrue();
        });
  }

  @Test
  public void close_whenNotConnected_shouldNotThrowAnyException() {
    Assertions.assertThatCode(subject::close).doesNotThrowAnyException();
  }

  @Test
  public void close_whenConnected_shouldCloseWebSocket() {
    server.enqueueConnect();

    server.gateway().onOpen((ws, r) -> ws.send("DUMMY"));
    subject.onGatewayPayload(m -> subject.close());

    subject.connect();

    Assert.thatWithinOneSecond(
        () -> {
          server.assertConnected();
          server.gateway().assertClosing(1000, "Closed.");
        });
  }

  @Test
  public void close_whenConnected_shouldCallListener() {
    CountDownLatch closeGate = new CountDownLatch(1);

    server.enqueueConnect();

    server.gateway().onOpen((ws, r) -> ws.send("DUMMY"));
    subject.onGatewayPayload(m -> subject.close());

    subject.onClose(closeGate::countDown);

    subject.connect();

    Assert.thatWithinOneSecond(closeGate::await);
  }

  @Test
  public void run_shouldConnect() {
    server.enqueueConnect();

    server.gateway().onOpen((ws, r) -> ws.send("DUMMY"));
    subject.onGatewayPayload(m -> subject.close());

    subject.run();

    Assert.thatWithinOneSecond(server::assertConnected);
  }

  @Test
  public void run_whenClose_shouldComplete() {
    server.enqueueConnect();

    server.gateway().onOpen((ws, r) -> ws.send("DUMMY"));
    subject.onGatewayPayload(m -> subject.close());

    Assert.thatWithinOneSecond(subject::run);
  }

  @Test
  public void run_whenNoClose_shouldNotComplete() {
    server.enqueueConnect();

    Assert.thatNotWithinOneSecond(subject::run);
  }

  private void assertThatNoServerRequest(Runnable r) {
    int beforeCount = server.getRequestCount();

    r.run();

    Assertions.assertThat(server.getRequestCount()).isEqualTo(beforeCount);
  }
}
