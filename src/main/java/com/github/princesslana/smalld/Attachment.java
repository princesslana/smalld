package com.github.princesslana.smalld;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okio.Buffer;

/**
 * An attachment for a multipart request. Content is read from the source provided for the
 * attachment on every call to {@link #getBytes()}.
 */
@RequiredArgsConstructor
public class Attachment {

  @Getter private final String filename;

  @Getter private final String mimeType;

  private final Supplier<InputStream> stream;

  /**
   * Create an instance with content from a byte array.
   *
   * @param filename the filename of this attachment
   * @param mimeType the mime type of this attachment
   * @param bytes the content of this attachment
   */
  public Attachment(String filename, String mimeType, byte[] bytes) {
    this(filename, mimeType, () -> new ByteArrayInputStream(bytes));
  }

  /**
   * Create an instance with content from the provided {@link File}.
   *
   * @param filename the filename of this attachment
   * @param mimeType the mime type of this attachment
   * @param file file to retrieve content of this attachment
   */
  public Attachment(String filename, String mimeType, File file) {
    this(filename, mimeType, supplyFrom(() -> new FileInputStream(file)));
  }

  /**
   * Create an instance with content from the provided {@link URL}.
   *
   * @param filename the filename of this attachment
   * @param mimeType the mime type of this attachment
   * @param url url to retrieve content of this attachment
   */
  public Attachment(String filename, String mimeType, URL url) {
    this(filename, mimeType, supplyFrom(url::openStream));
  }

  /**
   * Return the content as a byte array.
   *
   * @return the content
   */
  public byte[] getBytes() {
    try (Buffer buffer = new Buffer();
        InputStream is = stream.get()) {
      buffer.readFrom(is);
      return buffer.readByteArray();
    } catch (IOException e) {
      throw new SmallDException(e);
    }
  }

  private static Supplier<InputStream> supplyFrom(Callable<InputStream> c) {
    return () -> {
      try {
        return c.call();
      } catch (Exception e) {
        throw new SmallDException(e);
      }
    };
  }
}
