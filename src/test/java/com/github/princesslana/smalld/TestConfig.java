package com.github.princesslana.smalld;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestConfig {

  @Test
  public void getBaseUrl_whenDefaults_shouldBeV6Url() {
    Config cfg = Config.builder().build();
    Assertions.assertThat(cfg.getBaseUrl()).isEqualTo("https://discord.com/api/v6");
  }

  @Test
  public void getClock_whenDefaults_shouldNotBeNull() {
    Config cfg = Config.builder().build();
    Assertions.assertThat(cfg.getClock()).isNotNull();
  }
}
