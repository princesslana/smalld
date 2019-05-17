package com.github.princesslana.smalld;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRateLimitBucket {

  @Test
  public void equals_whenSelf_shouldBeTrue() {
    RateLimitBucket bucket = RateLimitBucket.from("/path");
    Assertions.assertThat(bucket.equals(bucket)).isTrue();
  }

  @Test
  public void equals_whenSameRequest_shouldBeTrue() {
    Assertions.assertThat(RateLimitBucket.from("/path").equals(RateLimitBucket.from("/path")))
        .isTrue();
  }

  @Test
  public void equals_whenNull_shouldBeFalse() {
    Assertions.assertThat(RateLimitBucket.from("/path").equals(null)).isFalse();
  }

  @Test
  public void equals_whenString_shouldBeFalse() {
    Assertions.assertThat(RateLimitBucket.from("/path").equals("/path")).isFalse();
  }

  @Test
  public void from_whenBulkDelete_shouldBeSameBucket() {
    Assertions.assertThat(RateLimitBucket.from("/channels/123/messages/bulk-delete"))
        .isEqualTo(RateLimitBucket.from("/channels/123/messages/bulk_delete"));
  }

  @Test
  public void from_whenMessageIdVaries_shouldBeSameBucket() {
    Assertions.assertThat(RateLimitBucket.from("/channels/123/messages/888"))
        .isEqualTo(RateLimitBucket.from("/channels/123/messages/999"));
  }

  @Test
  public void from_whenChannelIdVaries_shouldBeDifferentBucket() {
    Assertions.assertThat(RateLimitBucket.from("/channels/123/messages/999"))
        .isNotEqualTo(RateLimitBucket.from("/channels/456/messages/999"));
  }
}
