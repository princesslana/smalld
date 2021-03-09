package com.github.princesslana.smalld;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/** A {@link WebSocketListener} that logs all events received. */
@Slf4j
@RequiredArgsConstructor
public class LoggingWebSocketListener extends WebSocketListener {

  private final WebSocketListener wrapped;

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
