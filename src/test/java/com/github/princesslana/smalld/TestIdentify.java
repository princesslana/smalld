package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class TestIdentify extends ListenerTest<Identify> {

  @Mock private SequenceNumber sequenceNumber;

  @Override
  protected Identify createListener() {
    return new Identify(sequenceNumber);
  }

  @Test
  void whenReceiveHello_shouldSendIdentify() {
    sendToListener(Json.object().add("op", GatewayPayload.OP_HELLO));

    String sent = captureSentPayload();

    JsonAssertions.assertThatJson(sent)
        .and(
            j -> j.node("op").isEqualTo(GatewayPayload.OP_IDENTIFY),
            j -> j.node("d.token").isEqualTo(MOCK_TOKEN),
            j -> j.node("d.compress").isBoolean().isFalse(),
            j -> j.node("d.intents").isEqualTo(GatewayIntent.ALL),
            j -> j.node("d.shard").isArray().containsExactly(0, 1),
            j -> j.node("d.properties.$os").isNotNull(),
            j -> j.node("d.properties.$device").isEqualTo("SmallD"),
            j -> j.node("d.properties.$browser").isEqualTo("SmallD"));
  }

  @Test
  void whenReceiveHelloAfterReady_shouldSendResume() {
    Mockito.when(sequenceNumber.getLastSeen()).thenReturn(Optional.of(42L));

    sendToListener(
        Json.object()
            .add("op", GatewayPayload.OP_DISPATCH)
            .add("t", "READY")
            .add("d", Json.object().add("session_id", "abc123")));

    sendToListener(Json.object().add("op", GatewayPayload.OP_HELLO));

    String sent = captureSentPayload();

    JsonAssertions.assertThatJson(sent)
        .and(
            j -> j.node("op").isEqualTo(GatewayPayload.OP_RESUME),
            j -> j.node("d.token").isEqualTo(MOCK_TOKEN),
            j -> j.node("d.session_id").isEqualTo("abc123"),
            j -> j.node("d.seq").isEqualTo(42));
  }

  @Test
  void whenReceiveInvalidSession_shouldWaitAndIdentify()
      throws InterruptedException, ExecutionException {
    CompletableFuture.runAsync(
        () -> sendToListener(Json.object().add("op", GatewayPayload.OP_INVALID_SESSION)));

    CompletableFuture<String> sent = awaitSentPayload();

    Awaitility.await().atLeast(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).until(sent::isDone);
    JsonAssertions.assertThatJson(sent.get()).node("op").isEqualTo(GatewayPayload.OP_IDENTIFY);
  }
}
