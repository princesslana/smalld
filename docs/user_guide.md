# User Guide

This page outlines the API provided by SmallD.
It does not aim to outline Discord specifics, such as what are valid payloads, events, etc.

The following will be useful for reference when working with SmallD:

* [Discord Developer Documentation](https://discord.com/developers/docs/intro)
* [Javadocs](https://www.javadoc.io/doc/com.github.princesslana/smalld)

## Creating and Running 

The simplest way to run a bot is to use the `SmallD.run` method.
It should be passed a bot token and a lambda that will receive a `SmallD` instance.
If more configuration is desired `SmallD.run` can be passed an instance of `Config` rather
than the bot token.

```java
SmallD.run(myBotToken, (smalld) -> {
  // attach listeners and other functionality here
});
```

This method is a convenient wrapper around `SmallD.create` and `SmallD#run`.
If preferred, these methods can be called explicitly.
As with the static `run` method, a `Config` instance may be passed to `create` if
configuration beyond the bot token is required.

```java
SmallD smalld = SmallD.create(myBotToken);

// attach listeners and other functionality here

smalld.run();
```

## Configuration

SmallD configuration can be specified using the `Config` class and passed to `SmallD`.
The `Config` class provides a fluent style builder interface.

```java
Config config = Config.builder()
  .setToken(myBotToken)
  .setIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
  .setShard(1, 3)
  .build();

SmallD.run(config, myBot);
```

## Gateway Events

To listen to events from the Discord gateway use `Smalld#onGatewayPayload`.
This will pass the String that was sent from Discord to the listener you provide.

```java
smalld.onGatewayPayload((payload) -> {
  // parse payload and act upon it
});
```

To send a payload to the Discord gateway you can use `SmallD#sendGatewayPayload`.

## Resources

Discord resources can be requested by using the `get`, `post`, `put`, `patch`, and `delete` methods.
The response from Discord is returned as a `String`, with a `HttpException` being thrown when there
is a non-2xx response. A `RateLimitException` is thrown to indicate the request was rate limited.

All methods require a path be provided.
`post`, `put`, and `patch` also require a payload passed in as a `String`.
`post` may also be passed a number of `Attachment`s.


## Testing

`MockSmallD` is a class provided to help with unit testing Discord bots made with `SmallD`.
It allows you to register listeners and simulate payloads being sent from Discord.
It will record calls made to `SmallD` and allow you to verify that the expected calls where made.

A `MockSmallD` instance should be psased to your bot in the same way you would pass a `SmallD` instance.
Passing your bot a payload from discord is done using the `MockSmallD#receivePayload` method.

To verify the actions your bot took in response to this payload there are
`awaitSentRequest`, `awaitGatewayPayload`, and `awaitLifecycleEvent` methods.
All these methods return a `CompletableFuture` that can be examined for the expected behavior.

