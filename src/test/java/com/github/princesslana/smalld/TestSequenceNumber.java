package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestSequenceNumber {

  private SequenceNumber subject;

  @Mock private SmallD smalld;

  @Captor private ArgumentCaptor<Consumer<String>> listener;

  @BeforeEach
  public void subject() {
    subject = new SequenceNumber();
    subject.accept(smalld);

    Mockito.verify(smalld).onGatewayPayload(listener.capture());
  }

  @Test
  public void getLastSeen_whenInitialized_shouldBeAbsent() {
    Assertions.assertThat(subject.getLastSeen()).isEmpty();
  }

  @Test
  public void getLastSeen_whenNoSequenceNumber_shouldBeAbsent() {
    sendPayload(Json.object().add("op", 0).toString());
    Assertions.assertThat(subject.getLastSeen()).isEmpty();
  }

  @Test
  public void getLastSeen_whenSequenceNumber_shouldBeThatNumber() {
    sendPayload(Json.object().add("s", 42).toString());
    Assertions.assertThat(subject.getLastSeen()).isPresent().contains(42L);
  }

  @Test
  public void getLastSeen_whenNullSequenceNumber_shouldBePreviousNumber() {
    sendPayload(Json.object().add("s", 42).toString());
    sendPayload(Json.object().add("s", Json.NULL).toString());
    Assertions.assertThat(subject.getLastSeen()).isPresent().contains(42L);
  }

  private void sendPayload(String payload) {
    listener.getValue().accept(payload);
  }
}
