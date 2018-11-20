package com.github.princesslana.smalld.examples;

import com.github.princesslana.smalld.SmallD;

public class PingBot {

  public static void main(String[] args) {
    SmallD smalld = SmallD.create(System.getenv("SMALLD_TOKEN"));

    /*smalld.onWebsocketEvent(evt -> {
      if (evt.getType().equals("MESSAGE_CREATE")) {
        smalld.post("/message", "");
      }
    });*/

  }
}
