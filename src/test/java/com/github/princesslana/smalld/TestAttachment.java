package com.github.princesslana.smalld;

import java.io.File;
import java.nio.charset.Charset;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TestAttachment {

  @Test
  void getBytes_whenFile_shouldReturnContentsInBytes() throws Exception {
    Attachment a =
        new Attachment("", null, new File(getClass().getResource("multipart_input.txt").toURI()));

    Assertions.assertThat(a.getBytes()).isEqualTo("xyz\n".getBytes(Charset.forName("UTF-8")));
  }

  @Test
  void getBytes_whenFileNotFound_shouldThrowSmallDException() {
    Attachment a = new Attachment("", null, new File("notfound.txt"));
    Assertions.assertThatThrownBy(a::getBytes).isInstanceOf(SmallDException.class);
  }
}
