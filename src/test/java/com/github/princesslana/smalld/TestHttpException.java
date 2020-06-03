package com.github.princesslana.smalld;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TestHttpException {

  @Test
  void httpException_shouldIncludeInfoInMessage() {
    HttpException e = new HttpException(500, "STATUS", "BODY");

    Assertions.assertThat(e.getMessage()).contains("500").contains("STATUS").contains("BODY");
  }
}
