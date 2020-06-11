package com.github.princesslana.smalld;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestSmallD {

  private static final String MOCK_TOKEN = "Mock.Token";

  private SmallD subject;

  private CompletableFuture<Void> run;

  private WebSocketListener wsListener;

  @Mock private HttpClient http;

  @Mock private WebSocket webSocket;

  private InOrder inOrder;

  @BeforeEach
  public void sujbect() throws Exception {
    subject = new SmallD(Config.builder().setToken(MOCK_TOKEN).build(), http);

    inOrder = Mockito.inOrder(http);

    Mockito.lenient()
        .when(
            http.send(
                Mockito.eq("/gateway/bot"), Mockito.any(), Mockito.eq(Collections.emptyMap())))
        .thenReturn("{ \"url\" : \"wss://example.com\" }");

    run = CompletableFuture.runAsync(subject::run);

    wsListener = awaitConnection().get(1, TimeUnit.SECONDS);
  }

  @AfterEach
  public void close() {
    subject.close();
  }

  @Test
  void create_whenDefaults_shouldHaveDefaultConfig() {
    Assertions.assertThat(subject.getToken()).isEqualTo(MOCK_TOKEN);
    Assertions.assertThat(subject.getCurrentShard()).isEqualTo(0);
    Assertions.assertThat(subject.getNumberOfShards()).isEqualTo(1);
    Assertions.assertThat(subject.getIntents()).isEqualTo(GatewayIntent.UNPRIVILEGED);
  }

  @Test
  void run_whenMessage_shouldNotifyListener() throws Exception {
    CompletableFuture<String> msg = new CompletableFuture<>();
    subject.onGatewayPayload(msg::complete);

    wsListener.onMessage(webSocket, "TEST_MESSAGE");

    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(msg::isDone);
    Assertions.assertThat(msg.get()).isEqualTo("TEST_MESSAGE");
  }

  @Test
  void run_whenCloseOnFirstPayload_shouldRunSuccessfully() {
    subject.onGatewayPayload(p -> subject.close());

    wsListener.onMessage(webSocket, "");

    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(run::isDone);
  }

  @Test
  void run_whenClosing_shouldReconnect() {
    wsListener.onClosing(webSocket, 0, "");
    assertReconnect();
  }

  @Test
  void run_whenFailure_shouldReconnect() {
    wsListener.onFailure(webSocket, new Exception(), null);
    assertReconnect();
  }

  @Test
  void run_whenListenerException_shouldContinue() throws Exception {
    AtomicBoolean throwException = new AtomicBoolean(true);
    CompletableFuture<String> msg = new CompletableFuture<>();

    subject.onGatewayPayload(
        p -> {
          if (throwException.get()) {
            throw new RuntimeException();
          }
        });
    subject.onGatewayPayload(msg::complete);

    wsListener.onMessage(webSocket, "");

    Assertions.assertThat(msg.isDone()).isFalse();

    throwException.set(false);

    wsListener.onMessage(webSocket, "TEST_MESSAGE");

    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(msg::isDone);
    Assertions.assertThat(msg.get()).isEqualTo("TEST_MESSAGE");
  }

  private CompletableFuture<WebSocketListener> awaitConnection() {
    return CompletableFuture.supplyAsync(
        () -> {
          ArgumentCaptor<WebSocketListener> ws = ArgumentCaptor.forClass(WebSocketListener.class);
          inOrder.verify(http, Mockito.timeout(10000)).newWebSocket(Mockito.any(), ws.capture());
          return ws.getValue();
        });
  }

  private void assertReconnect() {
    CompletableFuture<WebSocketListener> reconnect = awaitConnection();
    Awaitility.await()
        .atLeast(4, TimeUnit.SECONDS)
        .atMost(6, TimeUnit.SECONDS)
        .until(reconnect::isDone);
  }
}
