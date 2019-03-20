package com.github.princesslana.smalld;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;

/** A {@link WebSocketListener} that logs all events received. */
public class LoggingWebSocketListener extends WebSocketListener {

  private final Logger log;

  private final WebSocketListener wrapped;

  /**
   * Create an instance that logs to the given logger and delegates to the provided {@link
   * WebSocketListener}.
   *
   * @param log the {@link Logger} to log events to
   * @param wrapped the listener to delegate events to
   */
  public LoggingWebSocketListener(Logger log, WebSocketListener wrapped) {
    this.log = log;
    this.wrapped = wrapped;
  }

  @Override
  public void onClosed(WebSocket ws, int code, String reason) {
    log.debug("WebSocket Closed: {}, {}", code, reason);
    wrapped.onClosed(ws, code, reason);
  }

  @Override
  public void onClosing(WebSocket ws, int code, String reason) {
    log.debug("WebSocket Closing: {}, {}", code, reason);
    wrapped.onClosing(ws, code, reason);
  }

  @Override
  public void onFailure(WebSocket ws, Throwable t, Response response) {
    log.debug("WebSocket Failure: {}", response, t);
    wrapped.onFailure(ws, t, response);
  }

  @Override
  public void onMessage(WebSocket ws, ByteString bytes) {
    log.debug("WebSocket Message (ByteString): {} bytes", bytes.size());
    wrapped.onMessage(ws, bytes);
  }

  @Override
  public void onMessage(WebSocket ws, String text) {
    log.debug("WebSocket Message (String): {}", text);
    wrapped.onMessage(ws, text);
  }

  @Override
  public void onOpen(WebSocket ws, Response response) {
    log.debug("WebSocket Open: {}", response);
    wrapped.onOpen(ws, response);
  }
}
