package com.github.princesslana.smalld;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

/**
 * Sends heartbeat payloads to the Discord Gateway. It will begin sending heartbeats after the HELLO
 * payload is received and reconnects if a heartbeat ack is not received. Sends a HEARTBEAT when a
 * HEARTBEAT event is received.
 */
@RequiredArgsConstructor
public class Heartbeat implements Consumer<SmallD> {

  private final SequenceNumber sequenceNumber;

  private Thread heartbeatThread = null;

  private volatile long heartbeatInterval;

  private volatile boolean ackReceived = false;

  @Override
  public void accept(SmallD smalld) {
    smalld.onGatewayPayload(
        s -> {
          GatewayPayload p = GatewayPayload.parse(s);

          switch (p.getOp()) {
            case GatewayPayload.OP_HELLO:
              onHello(smalld, p.getD());
              break;

            case GatewayPayload.OP_HEARTBEAT:
              onHeartbeat(smalld);
              break;

            case GatewayPayload.OP_HEARTBEAT_ACK:
              onHeartbeatAck();
              break;
          }
        });
  }

  private void onHello(SmallD smalld, JsonObject d) {
    heartbeatInterval = d.getInt("heartbeat_interval", -1);

    if (heartbeatThread == null || !heartbeatThread.isAlive()) {
      heartbeatThread = SmallD.DAEMON_THREAD_FACTORY.newThread(() -> runHeartbeatLoop(smalld));
      heartbeatThread.start();
    }
  }

  private void runHeartbeatLoop(SmallD smalld) {
    try {
      Thread.sleep(heartbeatInterval);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
      return;
    }

    while (true) {
      sendHeartbeat(smalld);

      try {
        Thread.sleep(heartbeatInterval);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
        return;
      }

      if (ackReceived) {
        ackReceived = false;
      } else {
        smalld.reconnect();
        break;
      }
    }
  }

  private void onHeartbeat(SmallD smalld) {
    sendHeartbeat(smalld);
  }

  private void onHeartbeatAck() {
    ackReceived = true;
  }

  private void sendHeartbeat(SmallD smalld) {
    smalld.sendGatewayPayload(
        Json.object()
            .add("op", GatewayPayload.OP_HEARTBEAT)
            .add("d", sequenceNumber.getLastSeen().map(Json::value).orElse(Json.NULL))
            .toString());
  }
}
