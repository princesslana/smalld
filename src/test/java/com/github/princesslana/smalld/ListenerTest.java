package com.github.princesslana.smalld;

import com.eclipsesource.json.JsonValue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

@ExtendWith(MockitoExtension.class)
public abstract class ListenerTest<T extends Consumer<SmallD>> {

  private static final int MAX_PAYLOAD_WAIT_MS = 10000;

  protected static final String MOCK_TOKEN = "Mock.Token";

  @Mock private SmallD smalld;

  private InOrder inOrder;

  private T subject;

  private Consumer<String> payloadListener;

  @BeforeEach
  public void subject() {
    inOrder = Mockito.inOrder(smalld);

    subject = createListener();

    payloadListener = capturePayloadListener(subject);

    Config config = Config.builder().setToken(MOCK_TOKEN).build();

    Mockito.lenient().when(smalld.getToken()).thenReturn(MOCK_TOKEN);
    Mockito.lenient().when(smalld.getCurrentShard()).thenReturn(config.getCurrentShard());
    Mockito.lenient().when(smalld.getNumberOfShards()).thenReturn(config.getNumberOfShards());
    Mockito.lenient().when(smalld.getIntents()).thenReturn(config.getIntents());
  }

  protected abstract T createListener();

  protected T getListener() {
    return subject;
  }

  protected void sendToListener(JsonValue json) {
    sendToListener(json.toString());
  }

  protected void sendToListener(String payload) {
    payloadListener.accept(payload);
  }

  private Consumer<String> capturePayloadListener(T listener) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<String>> payloadListener = ArgumentCaptor.forClass(Consumer.class);

    listener.accept(smalld);
    Mockito.verify(smalld).onGatewayPayload(payloadListener.capture());
    return payloadListener.getValue();
  }

  protected String captureSentPayload() {
    return captureSentPayload(Mockito.times(1));
  }

  protected String captureSentPayload(VerificationMode vm) {
    ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
    inOrder.verify(smalld, vm).sendGatewayPayload(sent.capture());
    return sent.getValue();
  }

  protected CompletableFuture<String> awaitSentPayload() {
    return CompletableFuture.supplyAsync(
        () -> captureSentPayload(Mockito.timeout(MAX_PAYLOAD_WAIT_MS)));
  }
}
