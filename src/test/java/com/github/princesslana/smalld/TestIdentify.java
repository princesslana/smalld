package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.github.princesslana.smalld.test.MockSmallD;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestIdentify {

  private Identify subject;

  private MockSmallD smalld;

  @Mock private SequenceNumber sequenceNumber;

  @BeforeEach
  void subject() {
    smalld = new MockSmallD();
    subject = new Identify(sequenceNumber);
    subject.accept(smalld);
  }

  @Test
  void whenReceiveHello_shouldSendIdentify() throws Exception {
    smalld.receivePayload(Json.object().add("op", GatewayPayload.OP_HELLO).toString());

    String sent = smalld.awaitSentPayload().get();

    JsonAssertions.assertThatJson(sent)
        .and(
            j -> j.node("op").isEqualTo(GatewayPayload.OP_IDENTIFY),
            j -> j.node("d.token").isEqualTo(MockSmallD.MOCK_TOKEN),
            j -> j.node("d.compress").isBoolean().isFalse(),
            j -> j.node("d.intents").isEqualTo(GatewayIntent.UNPRIVILEGED),
            j -> j.node("d.shard").isArray().containsExactly(0, 1),
            j -> j.node("d.properties.$os").isNotNull(),
            j -> j.node("d.properties.$device").isEqualTo("SmallD"),
            j -> j.node("d.properties.$browser").isEqualTo("SmallD"));
  }

  @Test
  void whenReceiveHelloAfterReady_shouldSendResume() throws Exception {
    Mockito.when(sequenceNumber.getLastSeen()).thenReturn(Optional.of(42L));

    smalld.receivePayload(
        Json.object()
            .add("op", GatewayPayload.OP_DISPATCH)
            .add("t", "READY")
            .add("d", Json.object().add("session_id", "abc123"))
            .toString());

    smalld.receivePayload(Json.object().add("op", GatewayPayload.OP_HELLO).toString());

    String sent = smalld.awaitSentPayload().get();

    JsonAssertions.assertThatJson(sent)
        .and(
            j -> j.node("op").isEqualTo(GatewayPayload.OP_RESUME),
            j -> j.node("d.token").isEqualTo(MockSmallD.MOCK_TOKEN),
            j -> j.node("d.session_id").isEqualTo("abc123"),
            j -> j.node("d.seq").isEqualTo(42));
  }

  @Test
  void whenReceiveInvalidSession_shouldWaitAndIdentify()
      throws InterruptedException, ExecutionException {
    CompletableFuture.runAsync(
        () ->
            smalld.receivePayload(
                Json.object().add("op", GatewayPayload.OP_INVALID_SESSION).toString()));

    CompletableFuture<String> sent = smalld.awaitSentPayload();

    Awaitility.await().atLeast(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).until(sent::isDone);
    JsonAssertions.assertThatJson(sent.get()).node("op").isEqualTo(GatewayPayload.OP_IDENTIFY);
  }
}
