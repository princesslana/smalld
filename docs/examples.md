# Examples

This page outlines examples of SmallD being used.
[For the Basics](#for-the-basics) outlines example code, designed to demonstrate SmallD in a small
amount of code.
[In the Wild](#in-the-wild) links to larger projects and bots making use of SmallD.


## For the Basics

There are example bots included in the github repository
[here.](https://github.com/princesslana/smalld/tree/main/src/main/java/com/github/princesslana/smalld/examples)
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

## In The Wild

* [**Disparse.**](https://github.com/BoscoJared/disparse)
  An ergonomic, simple, and easy-to-use command parsing and dispatching library for Discord bots
* [**StarMeUp.**](https://github.com/princesslana/star-me-up) Dead simple starboard bot.
* [**To Twenty Two.**](https://github.com/princesslana/to-twenty-two) A counting game, get to 22.

