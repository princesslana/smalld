package com.github.princesslana.smalld;

import java.time.Clock;

/** Config options for {@link SmallD}. */
public class Config {

  private String baseUrl;
  private final Clock clock;
  private final int currentShard;
  private final int numberOfShards;
  private final String token;
  private final int intents;

  private Config(Builder builder) {
    baseUrl = builder.baseUrl;
    clock = builder.clock;
    currentShard = builder.currentShard;
    numberOfShards = builder.numberOfShards;
    token = builder.token;
    intents = builder.intents;
  }

  /**
   * Returns the base url that resource requests should be sent to.
   *
   * @return the base url to use for resources
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Returns the {@link Clock} that is configured.
   *
   * @return source of current time
   */
  public Clock getClock() {
    return clock;
  }

  /**
   * Return what is configured as the current shard.
   *
   * @return the configuration for current shard
   */
  public int getCurrentShard() {
    return currentShard;
  }

  /**
   * Return the bitmask for the {@link GatewayIntent}s that are subscribed to.
   *
   * @return the bitasmk for intents that are subscribe to
   */
  public int getIntents() {
    return intents;
  }

  /**
   * Return what is configured as the number of shards.
   *
   * @return the number of shards configured
   */
  public int getNumberOfShards() {
    return numberOfShards;
  }

  /**
   * Returns the Discord bot token that is configured.
   *
   * @return the token
   */
  public String getToken() {
    return token;
  }

  /**
   * Creates a {@link Builder} that can be used to create an instance.
   *
   * @return a {@link Builder} that can build a {@code Config}
   */
  public static Builder builder() {
    return new Config.Builder();
  }

  /** {@code Builder} of {@code Config} instances. */
  public static class Builder {

    private static final String V6_BASE_URL = "https://discord.com/api/v6";
    private static final String V8_BASE_URL = "https://discord.com/api/v8";

    private String baseUrl = V8_BASE_URL;
    private Clock clock = Clock.systemUTC();
    private int currentShard = 0;
    private int numberOfShards = 1;
    private String token;
    private int intents = GatewayIntent.UNPRIVILEGED;

    private Builder() {}

    /**
     * Set the base URL to be used for reaching the Discord API. If not set this will default to
     * {@code https://discord.com/api/v8}
     *
     * @param baseUrl the base URL to be used to reach the Discord API
     * @return this
     */
    public Builder setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /**
     * Set the base URL to that of the v6 Discord API.
     *
     * @return this
     */
    public Builder v6() {
      return setBaseUrl(V6_BASE_URL);
    }

    /**
     * Set the base URL to that of the v8 Discord API.
     *
     * @return this
     */
    public Builder v8() {
      return setBaseUrl(V8_BASE_URL);
    }

    /**
     * Set the {@link Clock} to be used by {@link SmallD}.
     *
     * @param clock the Clock to use
     * @return this
     */
    public Builder setClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * Set the {@link GatewayIntent}s to subscribe to.
     *
     * @param intents the GatewayIntents to subscribe to
     * @return this
     */
    public Builder setIntents(GatewayIntent... intents) {
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
    public Builder setShard(int current, int number) {
      this.currentShard = current;
      this.numberOfShards = number;
      return this;
    }

    /**
     * Set the Discord bot token to be used by {@link SmallD}.
     *
     * @param token the token to use
     * @return this
     */
    public Builder setToken(String token) {
      this.token = token;
      return this;
    }

    /**
     * Build the {@code Config} instance.
     *
     * @return the built Config instance
     */
    public Config build() {
      return new Config(this);
    }
  }
}
