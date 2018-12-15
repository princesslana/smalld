package com.github.princesslana.smalld;

import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.range;
import static org.quicktheories.generators.SourceDSL.strings;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestHttpException {

  @Test
  public void httpException_shouldIncludeInfoInMessage() {
    qt().forAll(
            range(100, 599),
            strings().allPossible().ofLengthBetween(1, 255),
            strings().allPossible().ofLengthBetween(1, 1024))
        .checkAssert(
            (c, s, b) ->
                Assertions.assertThat(new HttpException(c, s, b).getMessage())
                    .contains(c.toString())
                    .contains(s)
                    .contains(b));
  }
}
