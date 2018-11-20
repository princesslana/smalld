package com.github.princesslana.smalld;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestConfiguration {

  @Test
  public void v6_shouldSetV6BaseUrl() {
    Configuration c = Configuration.v6("DUMMY_TOKEN");

    Assertions.assertThat(c.getBaseUrl()).isEqualTo("https://discordapp.com/api/v6");
  }

  @Test
  public void v6_shouldSetToken() {
    Configuration c = Configuration.v6("DUMMY_TOKEN");

    Assertions.assertThat(c.getToken()).isEqualTo("DUMMY_TOKEN");
  }
}
