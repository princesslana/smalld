package com.github.princesslana.smalld;

import java.util.Arrays;

/**
 * A GatewayIntent allows us to subscribe to only certain events from the Discord gateway. These are
 * combined into a bitmask that's sent with identification.
 */
public enum GatewayIntent {
  GUILDS(1 << 0),
  GUILD_MEMEBERS(1 << 1),
  GUILD_BANS(1 << 2),
  GUILD_EMOJIS(1 << 3),
  GUILD_INTEGRATIONS(1 << 4),
  GUILD_WEBHOOKS(1 << 5),
  GUILD_INVITES(1 << 6),
  GUILD_VOICE_STATES(1 << 7),
  GUILD_PRESENCES(1 << 8),
  GUILD_MESSAGES(1 << 9),
  GUILD_MESSAGE_REACTIONS(1 << 10),
  GUILD_MESSAGE_TYPING(1 << 11),
  DIRECT_MESSAGES(1 << 12),
  DIRECT_MESSAGE_REACTIONS(1 << 13),
  DIRECT_MESSAGE_TYPING(1 << 14);

  public static final int ALL = GatewayIntent.toMask(GatewayIntent.values());
  public static final int PRIVILEGED = GatewayIntent.toMask(GUILD_PRESENCES, GUILD_MEMEBERS);
  public static final int UNPRIVILEGED = ALL ^ PRIVILEGED;

  private final int mask;

  private GatewayIntent(int mask) {
    this.mask = mask;
  }

  /**
   * Combine a number of intents into a bitmask.
   *
   * @param intents the intents to include in the bitmask
   * @return the resulting bitmask from combining these intents
   */
  public static int toMask(GatewayIntent... intents) {
    return Arrays.stream(intents).mapToInt(g -> g.mask).reduce((x, y) -> x | y).orElse(0);
  }
}
