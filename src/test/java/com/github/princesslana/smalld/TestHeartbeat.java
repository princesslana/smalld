package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class TestHeartbeat extends ListenerTest<Heartbeat> {

  @Mock private SequenceNumber sequenceNumber;

  @Override
  protected Heartbeat createListener() {
    return new Heartbeat(sequenceNumber);
  }

  @Test
  void whenHelloReceived_shouldSendHeartbeat() {
    sendToListener(ready(500));
    assertHeartbeat(0, 1);
  }

  @Test
  void whenSecondHelloReceived_shouldCancelFirstHeartbeat() {
    sendToListener(ready(500));
    assertHeartbeat(0, 1);

    sendToListener(ready(1500));
    assertHeartbeat(1, 2);
  }

  @Test
  void whenSequenceNumber_shouldBeIncludedInHeartbeat() throws Exception {
    Mockito.when(sequenceNumber.getLastSeen()).thenReturn(Optional.of(42L));

    sendToListener(ready(500));

    String heartbeat = awaitSentPayload().get();
    JsonAssertions.assertThatJson(heartbeat).node("d").isEqualTo(42);
  }

  private JsonObject ready(int interval) {
    return Json.object().add("op", 10).add("d", Json.object().add("heartbeat_interval", interval));
  }

  private void assertHeartbeat(int minSeconds, int maxSeconds) {
    try {
      for (int i = 0; i < 2; i++) {
        CompletableFuture<String> sent = awaitSentPayload();
        Awaitility.await()
            .atLeast(minSeconds, TimeUnit.SECONDS)
            .atMost(maxSeconds, TimeUnit.SECONDS)
            .until(sent::isDone);
        JsonAssertions.assertThatJson(sent.get()).node("op").isEqualTo(GatewayPayload.OP_HEARTBEAT);
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
