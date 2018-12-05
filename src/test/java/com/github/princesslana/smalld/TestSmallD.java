package com.github.princesslana.smalld;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.eclipsesource.json.Json;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSmallD {

  private static final Logger LOG = LoggerFactory.getLogger(TestSmallD.class);

  private static final String TOKEN = "DUMMY_TOKEN";

  private SmallD subject;

  private MockWebServer server;

  @BeforeEach
  public void subject() {
    server = new MockWebServer();

    subject = new SmallD(TOKEN);
    subject.setBaseUrl(server.url("/api/v6").toString());
  }

  @AfterEach
  public void closeSmallD() {
    subject.close();
  }

  @AfterEach
  public void shutdownServer() {
    try {
      server.shutdown();
    } catch (IOException e) {
      LOG.warn("Failed to shutdown MockWebServer", e);
    }
  }

  @Test
  public void connect_shouldSendGetGatewayBotRequest() {
    enqueueGatewayBotResponse();

    subject.connect();

    RecordedRequest req = takeRequest(1);

    SoftAssertions.assertSoftly(
        s -> {
          s.assertThat(req.getMethod()).isEqualTo("GET");
          s.assertThat(req.getPath()).isEqualTo("/api/v6/gateway/bot");
        });
  }

  @Test
  public void connect_shouldIncudeTokenOnGetGatewayBotRequest() {
    enqueueGatewayBotResponse();

    subject.connect();

    RecordedRequest req = takeRequest(1);

    Assertions.assertThat(req.getHeader("Authorization")).isEqualTo("Bot DUMMY_TOKEN");
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
    enqueueGatewayBotResponse();
    WebSocketRecorder wsr = enqueueWebSocketResponse();

    subject.connect();

    assertInOneSecond(() -> wsr.assertOpened());
  }

  @Test
  public void onGatewayPayload_shouldForwardPayload() {
    String expected = "TEST MESSAGE";
    CompletableFuture<String> received = new CompletableFuture<>();

    enqueueGatewayBotResponse();
    WebSocketRecorder wsr = enqueueWebSocketResponse();

    wsr.onOpen((ws, r) -> ws.send(expected));

    subject.onGatewayPayload(received::complete);
    subject.connect();

    assertInOneSecond(() -> Assertions.assertThat(received.get()).isEqualTo(expected));
  }

  @Test
  public void sendGatewayPayload_shouldSendPayload() {
    String expected = "TEST MESSAGE";

    enqueueGatewayBotResponse();
    WebSocketRecorder wsr = enqueueWebSocketResponse();

    subject.connect();

    subject.sendGatewayPayload(expected);

    assertInOneSecond(
        () -> {
          wsr.assertOpened();
          wsr.assertMessage(expected);
        });
  }

  @Test
  public void await_shouldCompleteIfClosed() {
    Executors.newSingleThreadScheduledExecutor()
        .schedule(subject::close, 200, TimeUnit.MILLISECONDS);
    assertInOneSecond(subject::await);
  }

  @Test
  public void await_shouldNotCompleteIfNotClosed() {
    assertFails(() -> assertInOneSecond(subject::await));
  }

  @Test
  public void close_whenNotConnected_shouldNotThrowAnyException() {
    Assertions.assertThatCode(subject::close).doesNotThrowAnyException();
  }

  @Test
  public void close_whenConnected_shouldCloseWebSocket() {
    CountDownLatch openGate = new CountDownLatch(1);

    enqueueGatewayBotResponse();
    WebSocketRecorder wsr = enqueueWebSocketResponse();

    wsr.onOpen((ws, r) -> ws.send("DUMMY"));
    subject.onGatewayPayload(m -> subject.close());

    subject.connect();

    assertInOneSecond(
        () -> {
          subject.await();
          wsr.assertOpened();
          wsr.assertClosing(1000, "Closed.");
        });
  }

  private WebSocketRecorder enqueueWebSocketResponse() {
    WebSocketRecorder recorder = new WebSocketRecorder();
    server.enqueue(new MockResponse().withWebSocketUpgrade(recorder));
    return recorder;
  }

  private void enqueueGatewayBotResponse() {
    String wsUrl = "ws://" + server.getHostName() + ":" + server.getPort() + "/";
    String getGatewayBotResponse = Json.object().add("url", wsUrl).toString();

    server.enqueue(new MockResponse().setBody(getGatewayBotResponse));
  }

  private RecordedRequest takeRequest(int n) {
    Assertions.assertThat(server.getRequestCount()).isGreaterThanOrEqualTo(n);

    try {
      RecordedRequest r = null;

      for (int i = 0; i < n; i++) {
        r = server.takeRequest();
      }

      return r;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertFails(ThrowableAssert.ThrowingCallable t) {
    Assertions.assertThatExceptionOfType(AssertionError.class).isThrownBy(t);
  }

  private void assertInOneSecond(Executable exec) {
    assertTimeoutPreemptively(Duration.ofSeconds(1), exec);
  }
}
