# SmallD

SmallD is a minimalist Java library for making [Discord](https://discordapp.com) bots.
It doesn't aim to hide the Discord API from the developer, but instead to expose it for
use in a convenient way.
SmallD takes care of the just the necessities of communicating with the Discord API, leaving
a large amount of flexibility in the developer's hands.

Features considered in scope:

* Authentication, Identifying, Resuming
* Hearbeat
* Rate Limiting

Features considered out of scope:

* Serialization or deserialization os JSON messages/payloads
* Caching

## Multi-Language

The flexibility provided by the minimal API means SmallD is ideal for using from non-Java JVM
languages.
As such, there are examples of using SmallD from many languages included in this documentation.

There are getting started guides for:

* [Java](getting_started/java.md)

## Usage

For reference there's the [Javadocs](https://www.javadoc.io/doc/com.github.princesslana/smalld) and
always keep the [Discord Developer Documentation](https://discordapp.com/developers/docs/intro) handy.

If you just want to add it to your project right now SmallD is published to maven central.
So, it can be added with Maven or Gradle as below.
Replace `VERSION` with the version you wish to use.
The latest version is: ![Maven Central](https://img.shields.io/maven-central/v/com.github.princesslana/smalld.svg)

### Maven

```xml
  <dependency>
    <groupId>com.github.princesslana</groupId>
    <artifactId>smalld</artifactId>
    <version>VERSION</version>
  </dependency>
```

### Gradle

```groovy
compile 'com.github.princesslana:smalld:VERSION'
```

## Examples

There are example bots included in the github repository
[here.](https://github.com/princesslana/smalld/tree/master/src/main/java/com/github/princesslana/smalld/examples)
To run these you will need to setup your bot token in the `SMALLD_TOKEN` environment variable.
Then:

```bash
$ mvn compile exec:java \
    -Dexec.mainClass=com.github.princesslana.smalld.examples.<classname>
```

For example, to run PingBot:

```bash
$ mvn compile exec:java \
    -Dexec.mainClass=com.github.princesslana.smalld.examples.PingBot
```

There is also the [SmallD Examples](https://github.com/princesslana/smalld-examples) repository
that contains additional examples of using SmallD.

## Contributing

Head over to the [GitHub](https://github.com/princesslana/smalld).
