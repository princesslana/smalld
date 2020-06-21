# SmallD

SmallD is a minimalist Java library for making [Discord](https://discord.com) bots.
It doesn't aim to hide the Discord API from the developer, but instead to expose it for
use in a convenient way.
SmallD takes care of just the necessities of communicating with the Discord API, leaving
a large amount of flexibility in the developer's hands.

Features considered in scope:

* Authentication, Identifying, Resuming
* Hearbeat
* Rate Limiting

Features considered out of scope:

* Serialization and deserialization of JSON messages/payloads
* Caching

## Multi-Language

The flexibility provided by the minimal API means SmallD is ideal for using from non-Java JVM
languages.
As such, there are examples of using SmallD from many languages included in this documentation.

There are getting started guides for:

* [Java](getting_started/java.md)
* [Ruby](getting_started/ruby.md)

The SmallD concept has also been ported to:

* [Python](https://github.com/princesslana/smalld.py)

## Installing

SmallD is published to maven central.
It can be added with Maven or Gradle as below.
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

## Contributing

Head over to the [GitHub](https://github.com/princesslana/smalld) or the
[Discord Projects Hub](https://discord.gg/3aTVQtz) on Discord.
