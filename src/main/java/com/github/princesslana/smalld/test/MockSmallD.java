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

public class MockSmallD extends SmallD {

  public static final String MOCK_TOKEN = "Mock.Token";

  private final List<Consumer<String>> listeners = new ArrayList<>();

  private final BlockingQueue<String> sentPayloads = new ArrayBlockingQueue<>(100, true);

  private final BlockingQueue<SentRequest> sentRequests = new ArrayBlockingQueue<>(100, true);

  public MockSmallD() {
    super(Config.builder().setToken(MOCK_TOKEN).build());
  }

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

  public CompletableFuture<String> awaitSentPayload() {
    return take(sentPayloads);
  }

  @Override
  public String post(String path, String payload, Attachment... attachments) {
    sentRequests.add(new SentRequest("POST", path, payload));
    return "";
  }

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
