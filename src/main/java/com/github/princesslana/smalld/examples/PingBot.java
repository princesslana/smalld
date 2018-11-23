package com.github.princesslana.smalld.examples;

import com.github.princesslana.smalld.SmallD;

public class PingBot {

  public static void main(String[] args) {
    SmallD smalld = new SmallD(System.getenv("SMALLD_TOKEN"));

    /*smalld.onGatewayEvent((evt, conn) -> {
      if (evt.getType().equals("MESSAGE_CREATE")) {
        conn.post("/message", "");
      }
    });*/

    smalld.run();
  }
}
