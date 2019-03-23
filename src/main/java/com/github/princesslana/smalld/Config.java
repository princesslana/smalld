package com.github.princesslana.smalld;

import java.time.Clock;

/** Config options for {@link SmallD}. */
public class Config {

  private final Clock clock;
  private final String token;

  private Config(Builder builder) {
    clock = builder.clock;
    token = builder.token;
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

    private Clock clock = Clock.systemUTC();
    private String token;

    private Builder() {}

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
