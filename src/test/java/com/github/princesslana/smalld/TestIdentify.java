package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestIdentify {

  private static final String MOCK_TOKEN = "Mock.Token";

  private Identify subject;

  @Mock private SmallD smalld;

  @Mock private SequenceNumber sequenceNumber;

  private Consumer<String> listener;

  @BeforeEach
  public void subject() {
    subject = new Identify(sequenceNumber);

    listener = capturePayloadListener(subject);

    Mockito.lenient().when(smalld.getToken()).thenReturn(MOCK_TOKEN);
    Mockito.lenient().when(smalld.getCurrentShard()).thenReturn(0);
    Mockito.lenient().when(smalld.getNumberOfShards()).thenReturn(1);
  }

  @Test
  public void whenReceiveHello_shouldSendIdentify() {
    listener.accept(Json.object().add("op", GatewayPayload.OP_HELLO).toString());

    String sent = captureSentPayload();

    JsonAssertions.assertThatJson(sent)
        .and(
            j -> j.node("op").isEqualTo(GatewayPayload.OP_IDENTIFY),
            j -> j.node("d.token").isEqualTo(MOCK_TOKEN),
            j -> j.node("d.compress").isBoolean().isFalse(),
            j -> j.node("d.shard").isArray().containsExactly(0, 1),
            j -> j.node("d.properties.$os").isNotNull(),
            j -> j.node("d.properties.$device").isEqualTo("SmallD"),
            j -> j.node("d.properties.$browser").isEqualTo("SmallD"));
  }

  @Test
  public void whenReceiveHelloAfterReady_shouldSendResume() {
    Mockito.when(sequenceNumber.getLastSeen()).thenReturn(Optional.of(42L));

    listener.accept(
        Json.object()
            .add("op", GatewayPayload.OP_DISPATCH)
            .add("t", "READY")
            .add("d", Json.object().add("session_id", "abc123"))
            .toString());

    listener.accept(Json.object().add("op", GatewayPayload.OP_HELLO).toString());

    String sent = captureSentPayload();

    JsonAssertions.assertThatJson(sent)
        .and(
            j -> j.node("op").isEqualTo(GatewayPayload.OP_RESUME),
            j -> j.node("d.token").isEqualTo(MOCK_TOKEN),
            j -> j.node("d.session_id").isEqualTo("abc123"),
            j -> j.node("d.seq").isEqualTo(42));
  }

  @Test
  public void whenReceiveInvalidSession_shouldWaitAndIdentify()
      throws InterruptedException, ExecutionException {
    CompletableFuture.runAsync(
        () ->
            listener.accept(Json.object().add("op", GatewayPayload.OP_INVALID_SESSION).toString()));

    CompletableFuture<String> sent = awaitSentPayload();

    Awaitility.await().atLeast(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).until(sent::isDone);
    JsonAssertions.assertThatJson(sent.get()).node("op").isEqualTo(GatewayPayload.OP_IDENTIFY);
  }

  private Consumer<String> capturePayloadListener(Consumer<SmallD> bot) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<String>> listener = ArgumentCaptor.forClass(Consumer.class);

    bot.accept(smalld);
    Mockito.verify(smalld).onGatewayPayload(listener.capture());
    return listener.getValue();
  }

  private String captureSentPayload() {
    ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
    Mockito.verify(smalld).sendGatewayPayload(sent.capture());
    return sent.getValue();
  }

  private CompletableFuture<String> awaitSentPayload() {
    return CompletableFuture.supplyAsync(
        () -> {
          ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
          Mockito.verify(smalld, Mockito.timeout(10000)).sendGatewayPayload(sent.capture());
          return sent.getValue();
        });
  }
}
