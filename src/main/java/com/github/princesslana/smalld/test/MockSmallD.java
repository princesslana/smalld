package com.github.princesslana.smalld.test;

import com.github.princesslana.smalld.Attachment;
import com.github.princesslana.smalld.Config;
import com.github.princesslana.smalld.SmallD;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Allows the mocking of {@link SmallD} for use in unit testing. Provides the ability to emulate
 * receiving data as though it came from the Discord gateway, and tracks what HTTP requests and
 * gateway payloads are sent.
 */
public class MockSmallD extends SmallD {

  /** The token that the instance is conifgured with. */
  public static final String MOCK_TOKEN = "Mock.Token";

  private final List<Consumer<String>> listeners = new ArrayList<>();

  private final BlockingQueue<String> sentPayloads = new ArrayBlockingQueue<>(100, true);

  private final BlockingQueue<SentRequest> sentRequests = new ArrayBlockingQueue<>(100, true);

  /** Construct a {@code MockSmallD} instance. */
  public MockSmallD() {
    super(Config.builder().setToken(MOCK_TOKEN).build());
  }

  /**
   * Simulate a payload being received from the Discord gateway.
   *
   * @param payload the payload
   */
  public void receivePayload(String payload) {
    listeners.forEach(l -> l.accept(payload));
  }

  @Override
  public void onGatewayPayload(Consumer<String> listener) {
    listeners.add(listener);
  }

  @Override
  public void sendGatewayPayload(String payload) {
    sentPayloads.add(payload);
  }

  /**
   * Get the next payload that was sent to Discord.
   *
   * @return a {@code CompletableFuture} that will complete with the sent payload
   */
  public CompletableFuture<String> awaitSentPayload() {
    return take(sentPayloads);
  }

  @Override
  public String get(String path) {
    sentRequests.add(new SentRequest("GET", path, ""));
    return "";
  }

  @Override
  public String post(String path, String payload, Attachment... attachments) {
    sentRequests.add(new SentRequest("POST", path, payload));
    return "";
  }

  @Override
  public String put(String path, String payload) {
    sentRequests.add(new SentRequest("PUT", path, payload));
    return "";
  }

  @Override
  public String patch(String path, String payload) {
    sentRequests.add(new SentRequest("PATCH", path, payload));
    return "";
  }

  @Override
  public String delete(String path) {
    sentRequests.add(new SentRequest("DELETE", path, ""));
    return "";
  }

  /**
   * Get the next HTTP request that was sent to Discord.
   *
   * @return a {@code CompletableFuture} that will complete with the sent request.
   */
  public CompletableFuture<SentRequest> awaitSentRequest() {
    return take(sentRequests);
  }

  private <T> CompletableFuture<T> take(BlockingQueue<T> q) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return q.take();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
  }
}
