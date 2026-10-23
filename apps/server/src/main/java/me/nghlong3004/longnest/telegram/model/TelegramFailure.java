package me.nghlong3004.longnest.telegram.model;

import lombok.Getter;

/** Deliberately drops remote text, URLs, tokens and underlying exception causes. */
@Getter
public final class TelegramFailure extends RuntimeException {
  private final boolean fatal;
  private final long retrySeconds;

  public TelegramFailure(boolean fatal, long retrySeconds) {
    super(fatal ? "Telegram configuration/access unavailable" : "Telegram delivery unavailable");
    this.fatal = fatal;
    this.retrySeconds = Math.max(3, Math.min(86400, retrySeconds));
  }
}
