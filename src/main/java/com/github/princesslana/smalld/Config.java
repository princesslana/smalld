package com.github.princesslana.smalld;

import java.time.Clock;
import lombok.Builder;
import lombok.Data;

/** Config options for {@link SmallD}. */
@Data
@Builder
public class Config {

  private String baseUrl;
  private final Clock clock;
  private final int currentShard;
  private final int numberOfShards;
  private final String token;
  private final int intents;

  /** {@code Builder} of {@code Config} instances. */
  public static class ConfigBuilder {
    private static final String V6_BASE_URL = "https://discord.com/api/v6";
    private static final String V8_BASE_URL = "https://discord.com/api/v8";

    private String baseUrl = V8_BASE_URL;
    private Clock clock = Clock.systemUTC();
    private int currentShard = 0;
    private int numberOfShards = 1;
    private String token;
    private int intents = GatewayIntent.UNPRIVILEGED;

    /**
     * Set the base URL to be used for reaching the Discord API. If not set this will default to
     * {@code https://discord.com/api/v8}
     *
     * @param baseUrl the base URL to be used to reach the Discord API
     * @return this
     */
    public ConfigBuilder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /**
     * Set the base URL to that of the v6 Discord API.
     *
     * @return this
     */
    public ConfigBuilder v6() {
      return baseUrl(V6_BASE_URL);
    }

    /**
     * Set the base URL to that of the v8 Discord API.
     *
     * @return this
     */
    public ConfigBuilder v8() {
      return baseUrl(V8_BASE_URL);
    }

    /**
     * Set the {@link GatewayIntent}s to subscribe to.
     *
     * @param intents the GatewayIntents to subscribe to
     * @return this
     */
    public ConfigBuilder intents(GatewayIntent... intents) {
      this.intents = GatewayIntent.toMask(intents);
      return this;
    }

    /**
     * Configure the current shard and number of shards.
     *
     * @param current the current shard
     * @param number the number of shards
     * @return this
     */
    public ConfigBuilder shard(int current, int number) {
      this.currentShard = current;
      this.numberOfShards = number;
      return this;
    }
  }
}
