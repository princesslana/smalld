# Getting Started

This document provides a guide to getting started with SmallD.
It walks through setting up a ping bot.
This bot will respond with the message "pong" when a user sends "++ping".

To complete this project you should already have a token for your bot account.
To run your bot you should have invited it to a server also.
The fine folks of [discord.py](https://discordpy.readthedocs.io/) have a write up on how to do that [on their site](https://discordpy.readthedocs.io/en/latest/discord.html)

The full code, as will be completed by the end of this guide, is available on [GitHub](https://github.com/princesslana/smalld-gettingstarted/tree/master/java)


## Setup

We're going to use maven for our project.
So the first step we're going to do is setup maven and our `PingBot` class.
We'll just go for good 'ol "Hello World" to make sure we can compile and run successfully.

The first thing we do is setup our maven file.
In this file we:

* Setup the basic details of our project, such as its name (`artifactId`)
* Tell maven we're using Java 8 (feel free to use 11 or other versions if you wish)
* Add two dependencies
    * SmallD, of course. Update the version to be the latest: ![Maven Central](https://img.shields.io/maven-central/v/com.github.princesslana/smalld.svg)
    * minimal-json, as we'll need to parse JSON. You may like to use
    [Jackson](https://github.com/FasterXML/jackson) or
    [Gson](https://github.com/google/gson) for your own project
* Add the exec plugin. This allows us to run our bot via Maven

To set this up create a file `pom.xml` in the root of your project with the following contents:
```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.github.princesslana</groupId>
  <artifactId>smalld-gettingstarted</artifactId>
  <version>0-SNAPSHOT</version>

  <properties>
    <lib.smalld.version>!! UPDATE ME !!</lib.smalld.version>
    <lib.minimal-json.version>0.9.5</lib.minimal-json.version>
    <plugin.exec.version>1.6.0</plugin.exec.version>

    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.github.princesslana</groupId>
      <artifactId>smalld</artifactId>
      <version>${lib.smalld.version}</version>
    </dependency>
    <dependency>
      <groupId>com.eclipsesource.minimal-json</groupId>
      <artifactId>minimal-json</artifactId>
      <version>${lib.minimal-json.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>${plugin.exec.version}</version>
        <configuration>
          <mainClass>com.github.princesslana.smalld.start.PingBot</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

We also create our `PingBot` class that will have our `main` method.
We create the java file in `src/main/java/com/github/princesslana/smalld/start/PingBot.java`.
For now the contents are a "Hello World" program.

```java
package com.github.princesslana.smalld.start;

public class PingBot {
  public static void main(String[] args) {
    System.out.println("Hello World");
  }
}
```

We can now test our our maven compilation and run setup by executing
```bash
$ mvn compile exec:java
```

Towards the end of the maven output we should see our "Hello World" displayed.

## Connecting

The next stage of our project will be to make sure our bot is connecting to Discord.
This involves creating our `SmallD` instance and running it.

We update our `PingBot` class to read as follows:
```java
package com.github.princesslana.smalld.start;

import com.github.princesslana.smalld.SmallD;

public class PingBot {
  public static void main(String[] args) {
    try (SmallD smalld = SmallD.create(System.getProperty("smalld.token"))) {
      smalld.run();
    }
  }
}

```

In this code we:
* Create a `SmallD` instance, using a token read from a Java system property (we don't want to hardcode the token in our code)
* Ensure the creation is done within a try-with-resources block, to ensure it'll be closed correctly
* Run our `SmallD` instance

We can run our code as per the setup stage, but this time we want to provide our Bot token.
```bash
$ mvn compile exec:java -Dsmalld.token=<insert your token here>
```

Upon running this time we should see our Bot come online on Discord.

## Responding

Finally we can start making our PingBot do something.

We now want to setup a listener for gateway payloads.
We want it to recognize when a "ping" message is sent and respond by sending a "pong" message.
To do this we need to understand the [message create](https://discord.com/developers/docs/topics/gateway#message-create) event and the [create message](https://discord.com/developers/docs/resources/channel#create-message) resource.

We want our listener to:
* Parse the payload as JSON
* If it is a message create event with a message content of "ping"
  * Make a POST to the create message resource with the same channel id as the message create event and a content of "ping"

This leads to us having the following code:
```java
package com.github.princesslana.smalld.start;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.github.princesslana.smalld.SmallD;

public class PingBot {
  public static void main(String[] args) {
    try (SmallD smalld = SmallD.create(System.getProperty("smalld.token"))) {
      smalld.onGatewayPayload(
        p -> {
          JsonObject json = Json.parse(p).asObject();

          if (isMessageCreate(json) && getMessageContent(json).equals("++ping")) {
            String channelId = getChannelId(json);

            sendPong(smalld, channelId);
          }
        });

      smalld.run();
    }
  }
}
```

This will not compile yet, as we need to imlpement the methods we've used, but it does
outline the steps we wanted to take. Now we can proceed to implementing the methods.

**isMessageCreate**

By refering to the Discord API docs we know that this payload should have a `op` of zero to indicate and event, and an event type (`t`) equal to `MESSAGE_CREATE`.

```java
  private static boolean isMessageCreate(JsonObject json) {
    return json.getInt("op", -1) == 0 && json.getString("t", "").equals("MESSAGE_CREATE");
  }
```

**getMessageContent**

The message create payload is a Discord [message object](https://discord.com/developers/docs/resources/channel#message-object).
So to retrieve the message content, we need to check the `d.content` field of the payload our listener received.

```java
  private static String getMessageContent(JsonObject json) {
    return json.get("d").asObject().getString("content", "");
  }
```

**getChannelId**

Following on from `getMessageContent` we can find the channel id under `d.channel_id`.

```java
  public static String getChannelId(JsonObject json) {
    return json.get("d").asObject().getString("channel_id", "");
  }
```

**sendPong**

And lastly we get to sending the pong message in reply.
Checking the Discord documentation for the [create message](https://discord.com/developers/docs/resources/channel#create-message) endpoint we see we need to make a POST request.
We'll need to include the channel id in the path and send a JSON payload with "content" being "pong".

```java
  private static void sendPong(SmallD smalld, String channelId) {
    smalld.post(
      "/channels/" + channelId + "/messages",
      Json.object().add("content", "pong").toString());
  }
```

## Finally

Now our ping bot is complete.
We can run our bot with the token as we have done for previous steps.
After running we should see the bot online on Discord and should be 
able to make it respond by sending a `ping` message.

```bash
$ mvn compile exec:java -Dsmalld.token=<insert your token here>
```

If it does not work as you expecte take a check of the [GitHub repo](https://github.com/princesslana/smalld-gettingstarted/tree/master/java) to compare it to your code, and if that doesn't work feel free to get in touch via the details [here.](https://github.com/princesslana/smalld/blob/master/CONTRIBUTING.md)

Happy Coding!

