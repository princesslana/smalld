package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.github.princesslana.smalld.test.MockSmallD;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestSequenceNumber {

  private SequenceNumber subject;

  private MockSmallD smalld;

  @BeforeEach
  void subject() {
    smalld = new MockSmallD();
    subject = new SequenceNumber();
    subject.accept(smalld);
  }

  @Test
  void getLastSeen_whenInitialized_shouldBeAbsent() {
    Assertions.assertThat(subject.getLastSeen()).isEmpty();
  }

  @Test
  void getLastSeen_whenNoSequenceNumber_shouldBeAbsent() {
    smalld.receivePayload(Json.object().add("op", 0).toString());
    Assertions.assertThat(subject.getLastSeen()).isEmpty();
  }

  @Test
  void getLastSeen_whenSequenceNumber_shouldBeThatNumber() {
    smalld.receivePayload(Json.object().add("s", 42).toString());
    Assertions.assertThat(subject.getLastSeen()).isPresent().contains(42L);
  }

  @Test
  void getLastSeen_whenNullSequenceNumber_shouldBePreviousNumber() {
    smalld.receivePayload(Json.object().add("s", 42).toString());
    smalld.receivePayload(Json.object().add("s", Json.NULL).toString());
    Assertions.assertThat(subject.getLastSeen()).isPresent().contains(42L);
  }
}
