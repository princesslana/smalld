package com.github.princesslana.smalld;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TestConfig {

  @Test
  void getBaseUrl_whenDefaults_shouldBeV6Url() {
    Config cfg = Config.builder().build();
    Assertions.assertThat(cfg.getBaseUrl()).isEqualTo("https://discord.com/api/v8");
  }

  @Test
  void getClock_whenDefaults_shouldNotBeNull() {
    Config cfg = Config.builder().build();
    Assertions.assertThat(cfg.getClock()).isNotNull();
  }

  @Test
  void getIntents_whenDefaults_shouldBeUnprivileged() {
    Config cfg = Config.builder().build();
    Assertions.assertThat(cfg.getIntents()).isEqualTo(GatewayIntent.UNPRIVILEGED);
  }

  @Test
  void getIntents_whenIntentsSet_shouldBeSet() {
    Config cfg =
        Config.builder()
            .intents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
            .build();

    Assertions.assertThat(cfg.getIntents()).isEqualTo(1 << 9 | 1 << 12);
  }
}
