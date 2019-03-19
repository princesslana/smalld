package com.github.princesslana.smalld;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLoggingWebSocketListener {

  private static final Logger LOG = LoggerFactory.getLogger(TestLoggingWebSocketListener.class);

  private WebSocketListenerSpy spy;
  private WebSocketListener subject;

  @BeforeEach
  public void subject() {
    spy = new WebSocketListenerSpy();
    subject = new LoggingWebSocketListener(LOG, spy);
  }

  @Test
  public void onClosed_shouldDelegateToWrapped() {
    subject.onClosed(null, 0, null);
    Assertions.assertThat(spy.onClosedCalled).isTrue();
  }

  @Test
  public void onClosing_shouldDelegateToWrapped() {
    subject.onClosing(null, 0, null);
    Assertions.assertThat(spy.onClosingCalled).isTrue();
  }

  @Test
  public void onFailure_shouldDelegateToWrapped() {
    subject.onFailure(null, null, null);
    Assertions.assertThat(spy.onFailureCalled).isTrue();
  }

  @Test
  public void onMessageByteString_shouldDelegateToWrapped() {
    subject.onMessage(null, ByteString.of());
    Assertions.assertThat(spy.onMessageByteStringCalled).isTrue();
  }

  @Test
  public void onMessageString_shouldDelegateToWrapped() {
    subject.onMessage(null, "");
    Assertions.assertThat(spy.onMessageStringCalled).isTrue();
  }

  @Test
  public void onOpen_shouldDelegateToWrapped() {
    subject.onOpen(null, null);
    Assertions.assertThat(spy.onOpenCalled).isTrue();
  }

  private static class WebSocketListenerSpy extends WebSocketListener {

    public boolean onClosedCalled = false;
    public boolean onClosingCalled = false;
    public boolean onFailureCalled = false;
    public boolean onMessageByteStringCalled = false;
    public boolean onMessageStringCalled = false;
    public boolean onOpenCalled = false;

    @Override
    public void onClosed(WebSocket ws, int code, String reason) {
      onClosedCalled = true;
    }

    @Override
    public void onClosing(WebSocket ws, int code, String reason) {
      onClosingCalled = true;
    }

    @Override
    public void onFailure(WebSocket ws, Throwable t, Response response) {
      onFailureCalled = true;
    }

    @Override
    public void onMessage(WebSocket ws, ByteString bytes) {
      onMessageByteStringCalled = true;
    }

    @Override
    public void onMessage(WebSocket ws, String text) {
      onMessageStringCalled = true;
    }

    @Override
    public void onOpen(WebSocket ws, Response response) {
      onOpenCalled = true;
    }
  }
}
