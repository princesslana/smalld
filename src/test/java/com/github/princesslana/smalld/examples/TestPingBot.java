package com.github.princesslana.smalld.examples;

import com.eclipsesource.json.Json;
import com.github.princesslana.smalld.GatewayPayload;
import com.github.princesslana.smalld.test.MockSmallD;
import com.github.princesslana.smalld.test.SentRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestPingBot {

  private PingBot subject;

  private MockSmallD smalld = new MockSmallD();

  private static final String MOCK_CHANNEL_ID = "1234567890";

  @BeforeEach
  void subject() {
    smalld = new MockSmallD();
    subject = new PingBot();
    subject.accept(smalld);
  }

  @Test
  void subject_whenReceivingPing_shouldPong() throws Exception {
    smalld.receivePayload(messageCreate("++ping"));

    CompletableFuture<SentRequest> futureSent = smalld.awaitSentRequest();

    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(futureSent::isDone);

    SentRequest sent = futureSent.get();

    Assertions.assertThat(sent.getMethod()).isEqualTo("POST");
    Assertions.assertThat(sent.getPath()).isEqualTo("/channels/" + MOCK_CHANNEL_ID + "/messages");
    JsonAssertions.assertThatJson(sent.getPayload()).node("content").isEqualTo("pong");
  }

  @Test
  void subject_whenReceivingPlonk_shouldNotPong() throws Exception {
    smalld.receivePayload(messageCreate("++plonk"));

    CompletableFuture<SentRequest> futureSent = smalld.awaitSentRequest();

    Assertions.assertThatThrownBy(
            () -> Awaitility.await().atMost(1, TimeUnit.SECONDS).until(futureSent::isDone))
        .isInstanceOf(ConditionTimeoutException.class);
  }

  private String messageCreate(String content) {
    return Json.object()
        .add("op", GatewayPayload.OP_DISPATCH)
        .add("t", "MESSAGE_CREATE")
        .add("d", Json.object().add("content", content).add("channel_id", MOCK_CHANNEL_ID))
        .toString();
  }
}
