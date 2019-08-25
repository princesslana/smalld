# Getting Started with Ruby

This document provides a guide for getting started using Ruby with SmallD.
It walks through the steps for setting up a bot that will respond with "pong" when a
user sends "++ping".

Before starting you should have a token created for your bot account and invited it
to a server.
The fine folks of [discord.py](https://discordpy.readthedocs.io/) have a write up on how to do that [on their site](https://discordpy.readthedocs.io/en/latest/discord.html)

The complete code from this guide can be found on [GitHub](https://github.com/princesslana/smalld-gettingstarted/tree/master/ruby)


## JRuby

To make use of SmallD from Ruby we need to be running as version of [JRuby](https://jruby.org).
Installation instructions can be found on their site or you can make use of a
ruby environment manager such as (my favorite) [rbenv](https://github.com/rbenv/rbenv) or [rvm](https://rvm.io).


## Plumbing

Before starting on the code we'll setup the plumbing for our project.
This will be [Bundler](https://bundler.io) and [Rake](https://ruby.github.io/rake/).

Bundler will manage our dependencies for us, and Rake will make life easier for us when wanting
to run our bot.

### Gemspec

We start with our gemspec file.
We define some information about our project, such as its name and author.
The platform is restricted to `java` as we'll need a JVM for SmallD.

The requirements we add are:

* **SmallD** as that's what we'll be using for our Bot
* **slf4j-simple** to have slf4j log to standard output
* **jar-dependencies** is needed for JRuby to be able to resolve jar dependencies
* **rake** that we will use to make it easier for us to run our Bot

```ruby
Gem::Specification.new do |s|
  s.name = 'smalld-gettingstarted-ruby'
  s.authors = ['Princess Lana']
  s.version = '0.0.0'
  s.summary = 'Getting started with SmallD and JRuby'

  s.platform = 'java'

  s.requirements << 'jar com.github.princesslana, smalld, 0.1.0'
  s.requirements << 'jar org.slf4j, slf4j-simple, 1.7.25'

  s.add_development_dependency 'jar-dependencies', '0.4.0'
  s.add_development_dependency 'rake', '12.3.2'
end
```

### Gemfile

`Gemfile` follows trivially since we specify out dependencies in the gemspec.

```ruby
source 'https://rubygems.org'

gemspec
```

### Rakefile

We define two tasks in our `Rakefile`.

* **install_jars** This is needed our integration between JRUby and Java. It will download and setup
    our jar dependencies.
* **run:pingbot** This will run our bot.

In the `run:pingbot` task we pass an extra parameter through to JRuby.
The `-J` means to pass this parameter to Java, and the `-D` is the option for setting a
Java system property. The property we set turns up the logging level so that we have a better
understanding of what's going on.

```
require 'jars/installer'

task :install_jars do
  Jars::Installer.vendor_jars!
end

namespace :run do
  task :pingbot do
    ruby "-J-Dorg.slf4j.simpleLogger.defaultLogLevel=debug lib/pingbot.rb"
  end
end
```

At this point in time we should be able to fetch and install our dependencies.

```bash
$ bundle install
$ bundle exec rake install_jars
```

The first command will fetch and install our ruby dependencies, the second will do it for our
jar dependencies.

## Connecting

Now, we can start to code our Bot.
The first thing we'll do is make sure we can connect to Discord using `SmallD`.
This makes sure we can use all our requirements before we start working on our logic.

In `lib/pingbot.rb`:

```ruby
require_relative 'smalld-gettingstarted-ruby_jars'
java_import com.github.princesslana.smalld.SmallD

SmallD.run(ENV['SMALLD_TOKEN']) do |smalld|
  # Our logic will go here
end
```

Now we can run this as follows:

```bash
$ SMALLD_TOKEN=<your token here> bundle exec rake run:pingbot
```

You should seem some log messages, and you should see your bot come online in the server
you had invited it to.
You will need to quit running the bot with `<Ctrl-C>`.

## Responding

Now that we can connect to Discord, we can listen for message created events and respond to those
that are ping messages.
To do this we need to understand the [message create](https://discordapp.com/developers/docs/topics/gateway#message-create) event and the [create message](https://discordapp.com/developers/docs/resources/channel#create-message) resource.

The logic we add works as follows:

* Attach a listener for events from the Discord Gateway using `onGatewayPayload`.
* Upon receiving a payload we:
    * Parse it as JSON,
    * Check if it is a message create event
    * Check if the message content is `++ping`
    * Respond to the message with `pong`

```ruby
require_relative 'smalld-gettingstarted-ruby_jars'
java_import com.github.princesslana.smalld.SmallD
require 'json'

SmallD.run(ENV['SMALLD_TOKEN']) do |smalld|
  smalld.onGatewayPayload do |p_str|
    p_json = JSON.parse p_str

    if is_message_create?(p_json) && message_content(p_json) == '++ping'
      channel_id = p_json['d']['channel_id']

      smalld.post "/channels/#{channel_id}/messages", { content: 'pong' }.to_json 
    end
  end
end
```

But, this code isn't complete yet, as we need to implement the `is_message_create?` and
`message_content` methods.
By consulting the Discord docs we can find out what fields we need to use to do so,
and implement these methods as follows:

```ruby
def is_message_create?(json)
  json['op'] == 0 && json['t'] == 'MESSAGE_CREATE'
end

def message_content(json)
  json['d']['content']
end
```

## Finally

Now our ping bot is complete. We can run it with the command:

```bash
$ SMALLD_TOKEN=<your token here> bundle exec rake run:pingbot
```

If it does not work as you expecte take a check of the [GitHub repo](https://github.com/princesslana/smalld-gettingstarted/tree/master/ruby) to compare it to your code, and if that doesn't work feel free to get in touch via the details [here.](https://github.com/princesslana/smalld/blob/master/CONTRIBUTING.md)

Happy Coding!

