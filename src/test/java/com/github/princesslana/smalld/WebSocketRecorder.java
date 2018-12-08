package com.github.princesslana.smalld;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import net.javacrumbs.jsonunit.assertj.JsonAssert.ConfigurableJsonAssert;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

public class WebSocketRecorder extends WebSocketListener {

  private final BlockingQueue<Object> received = new LinkedBlockingQueue<>();

  private BiConsumer<WebSocket, Response> delegateOnOpen = (ws, m) -> {};

  public void onOpen(BiConsumer<WebSocket, Response> c) {
    delegateOnOpen = c;
  }

  @Override
  public void onOpen(WebSocket ws, Response response) {
    received.add(new Open());
    delegateOnOpen.accept(ws, response);
  }

  @Override
  public void onMessage(WebSocket ws, String message) {
    received.add(new Message(message));
  }

  @Override
  public void onClosing(WebSocket ws, int status, String reason) {
    received.add(new Closing(status, reason));
  }

  public void assertOpened() throws InterruptedException {
    assertThatNext().isEqualTo(new Open());
  }

  public void assertMessage(String message) throws InterruptedException {
    assertThatNext().isEqualTo(new Message(message));
  }

  public ConfigurableJsonAssert assertJsonMessage() throws InterruptedException {
    Object next = received.take();
    Assertions.assertThat(next).isInstanceOf(Message.class);
    return JsonAssertions.assertThatJson(((Message) next).getText());
  }

  public void assertClosing(int status, String reason) throws InterruptedException {
    assertThatNext().isEqualTo(new Closing(status, reason));
  }

  public ObjectAssert<Object> assertThatNext() throws InterruptedException {
    return Assertions.assertThat(received.take());
  }

  private static class Open {
    @Override
    public String toString() {
      return "Open()";
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      return other instanceof Open;
    }
  }

  private static class Message {
    private final String message;

    public Message(String message) {
      this.message = message;
    }

    public String getText() {
      return message;
    }

    @Override
    public String toString() {
      return "Message(" + Objects.toString(message) + ")";
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!(other instanceof Message)) {
        return false;
      }

      Message rhs = (Message) other;

      return Objects.equals(message, rhs.message);
    }
  }

  private static class Closing {
    private final int status;
    private final String reason;

    public Closing(int status, String reason) {
      this.status = status;
      this.reason = reason;
    }

    @Override
    public String toString() {
      return String.format("Closing(%d, %s)", status, Objects.toString(reason));
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!(other instanceof Closing)) {
        return false;
      }

      Closing rhs = (Closing) other;

      return status == rhs.status && Objects.equals(reason, rhs.reason);
    }
  }
}
