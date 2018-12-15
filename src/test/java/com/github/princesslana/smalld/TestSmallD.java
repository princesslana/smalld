package com.github.princesslana.smalld;

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
    String expected = "TEST MESSAGE";

    server.connect(subject);

    subject.sendGatewayPayload(expected);

    Assert.thatWithinOneSecond(() -> server.gateway().assertMessage(expected));
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

  @Test
  public void get_whenHttp300_shouldThrowHttpException() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(300));

    Assertions.assertThatExceptionOfType(HttpException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @Test
  public void get_whenHttp400_shouldThrowClientException() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(400));

    Assertions.assertThatExceptionOfType(HttpException.ClientException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @Test
  public void get_whenHttp429_shouldThrowRateLimitException() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(429));

    Assertions.assertThatExceptionOfType(HttpException.RateLimitException.class)
        .isThrownBy(() -> subject.get("/test/url"));
  }

  @Test
  public void get_whenHttp500_shouldThrowServerException() {
    server.connect(subject);
    server.enqueue(new MockResponse().setResponseCode(500));

    Assertions.assertThatExceptionOfType(HttpException.ServerException.class)
        .isThrownBy(() -> subject.get("/test/url"));
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
        .hasFieldOrPropertyWithValue("message", "Server Error");
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
    String payload = "TEST MESSAGE";

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
    CountDownLatch openGate = new CountDownLatch(1);

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
}
