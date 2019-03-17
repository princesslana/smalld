# SmallD

[![CircleCI](https://circleci.com/gh/princesslana/smalld/tree/master.svg?style=svg)](https://circleci.com/gh/princesslana/smalld/tree/master)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=com.github.princesslana%3Asmalld&metric=sqale_index)](https://sonarcloud.io/dashboard?id=com.github.princesslana%3Asmalld)
[![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.github.princesslana%3Asmalld&metric=coverage)](https://sonarcloud.io/dashboard?id=com.github.princesslana%3Asmalld)

SmallD aims to be a minmalist client for the Discord API.
It aims to let you use the Discord API, without hiding or abstracting it.

Features considered in scope:
* Authentication, Identifying, Resuming
* Hearbeat
* Rate Limiting

Features considered out of scope:
* Serialization or deserialization os JSON messages/payloads
* Caching

## Usage

Also see https://princesslana.github.io/smalld.

**Latest Release:** 
![Maven Central](https://img.shields.io/maven-central/v/com.github.princesslana/smalld.svg)
[![Javadocs](http://javadoc.io/badge/com.github.princesslana/smalld.svg)](http://javadoc.io/doc/com.github.princesslana/smalld)

Add as a dependency to maven, using the latest release version:
```xml
  <dependency>
    <groupId>com.github.princesslana</groupId>
    <artifactId>smalld</artifactId>
    <version>LASTEST_VERSION</version>
  </dependency>
```

### Development Version

Use [jitpack](https://jitpack.io/#princesslana/smalld).

Add the repository:
```xml
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
```

Add the dependency:
```xml
  <dependency>
    <groupId>com.github.princesslana</groupId>
    <artifactId>smalld</artifactId>
    <version>master-SNAPSHOT</version>
  </dependency>
```

## Development

See the [contributing guide](CONTRIBUTING.md).

To run the junit tests:
```bash
$ mvn test
```

To run the code formatter:
```bash
$ mvn spotless:apply
```

To run the full set of verifications (including test coverage and style checks):
```bash
$ mvn verify
```

