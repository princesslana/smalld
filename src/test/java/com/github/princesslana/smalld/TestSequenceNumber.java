package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestSequenceNumber extends ListenerTest<SequenceNumber> {

  @Override
  protected SequenceNumber createListener() {
    return new SequenceNumber();
  }

  @Test
  void getLastSeen_whenInitialized_shouldBeAbsent() {
    Assertions.assertThat(getListener().getLastSeen()).isEmpty();
  }

  @Test
  void getLastSeen_whenNoSequenceNumber_shouldBeAbsent() {
    sendToListener(Json.object().add("op", 0));
    Assertions.assertThat(getListener().getLastSeen()).isEmpty();
  }

  @Test
  void getLastSeen_whenSequenceNumber_shouldBeThatNumber() {
    sendToListener(Json.object().add("s", 42));
    Assertions.assertThat(getListener().getLastSeen()).isPresent().contains(42L);
  }

  @Test
  void getLastSeen_whenNullSequenceNumber_shouldBePreviousNumber() {
    sendToListener(Json.object().add("s", 42));
    sendToListener(Json.object().add("s", Json.NULL));
    Assertions.assertThat(getListener().getLastSeen()).isPresent().contains(42L);
  }
}
