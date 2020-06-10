package com.github.princesslana.smalld;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  @BeforeEach
  public void sujbect() {
    subject = new SmallD(Config.builder().setToken(MOCK_TOKEN).build(), http);

    Mockito.lenient()
        .when(
            http.send(
                Mockito.eq("/gateway/bot"), Mockito.any(), Mockito.eq(Collections.emptyMap())))
        .thenReturn("{ \"url\" : \"wss://example.com\" }");

    run = CompletableFuture.runAsync(subject::run);

    ArgumentCaptor<WebSocketListener> ws = ArgumentCaptor.forClass(WebSocketListener.class);
    Mockito.verify(http, Mockito.timeout(1000)).newWebSocket(Mockito.any(), ws.capture());
    wsListener = ws.getValue();
  }

  @Test
  void create_whenDefaults_shouldHaveDefaultConfig() {
    Assertions.assertThat(subject.getToken()).isEqualTo(MOCK_TOKEN);
    Assertions.assertThat(subject.getCurrentShard()).isEqualTo(0);
    Assertions.assertThat(subject.getNumberOfShards()).isEqualTo(1);
    Assertions.assertThat(subject.getIntents()).isEqualTo(GatewayIntent.UNPRIVILEGED);
  }

  @Test
  void run_whenCloseOnFirstPayload_shouldRunSuccessfully() {
    subject.onGatewayPayload((p) -> subject.close());

    wsListener.onMessage(webSocket, "");

    Awaitility.await().atMost(5, TimeUnit.SECONDS).until(run::isDone);
  }
}
