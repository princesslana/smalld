package com.github.princesslana.smalld;

import java.util.Optional;
import java.util.function.Consumer;

/** Tracks the last seen sequence number. */
public class SequenceNumber implements Consumer<SmallD> {

  private Long lastSeen;

  @Override
  public void accept(SmallD smalld) {
    smalld.onGatewayPayload(
        s -> {
          GatewayPayload.parse(s).getS().ifPresent(this::setLastSeen);
        });
  }

  /**
   * Return the last seen sequence number, if there is one.
   *
   * @return the last seen sequence number or {@code empty()} if none
   */
  public Optional<Long> getLastSeen() {
    return Optional.ofNullable(lastSeen);
  }

  private void setLastSeen(Long lastSeen) {
    this.lastSeen = lastSeen;
  }
}
