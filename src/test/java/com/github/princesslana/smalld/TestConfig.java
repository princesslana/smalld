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

  @Test
  public void getIntents_whenDefaults_shouldBeAll() {
    Config cfg = Config.builder().build();
    Assertions.assertThat(cfg.getIntents()).isEqualTo(GatewayIntent.ALL);
  }

  @Test
  public void getIntents_whenIntentsSet_shouldBeSet() {
    Config cfg =
        Config.builder()
            .setIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
            .build();

    Assertions.assertThat(cfg.getIntents()).isEqualTo(1 << 9 | 1 << 12);
  }
}
