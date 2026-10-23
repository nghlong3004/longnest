package me.nghlong3004.longnest.telegram.model;

import lombok.Getter;

/** No generated toString: a bot token must never appear in diagnostics. */
@Getter
public final class TelegramSettings {
  private final String token;
  private final long user;
  private final String namespace;

  public TelegramSettings(String token, long user) {
    if (token == null || !token.matches("[1-9][0-9]{0,19}:[A-Za-z0-9_-]{20,100}")
        || user <= 0 || user > (1L << 52) - 1) {
      throw new IllegalArgumentException("Telegram requires a valid token and one numeric user ID");
    }
    try {
      if (Long.parseLong(token.substring(0, token.indexOf(':'))) <= 0) {
        throw new IllegalArgumentException("Telegram token ID is invalid");
      }
    } catch (NumberFormatException invalid) {
      throw new IllegalArgumentException("Telegram token ID is invalid");
    }
    this.token = token;
    this.user = user;
    namespace = "tg:" + token.substring(0, token.indexOf(':')) + ":";
  }
}
