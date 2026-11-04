package me.nghlong3004.longnest.integration.telegram;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionRevision;
import me.nghlong3004.longnest.model.enums.TransactionField;

/** An edit snapshot; transitions enforce which data is valid in each phase. */
record TelegramEditSession(
    String chat,
    UUID id,
    int anchor,
    TransactionRevision before,
    TransactionField field,
    TransactionData proposed,
    Phase phase,
    Instant expires) {

  enum Phase {
    CARD,
    MENU,
    FIELD,
    INPUT,
    PREVIEW,
    DELETE
  }

  TelegramEditSession {
    Objects.requireNonNull(chat);
    Objects.requireNonNull(id);
    Objects.requireNonNull(phase);
    Objects.requireNonNull(expires);
    if (anchor <= 0
        || (phase == Phase.CARD) != (before == null)
        || ((phase == Phase.INPUT || phase == Phase.PREVIEW) != (field != null))
        || (phase == Phase.PREVIEW) != (proposed != null)
        || (before != null && !id.equals(before.transaction().id()))) {
      throw new IllegalArgumentException("Invalid transaction edit state");
    }
  }

  static TelegramEditSession card(String chat, UUID id, int anchor, Instant expires) {
    return new TelegramEditSession(chat, id, anchor, null, null, null, Phase.CARD, expires);
  }

  TelegramEditSession openMenu(TransactionRevision revision, int messageId, Instant deadline) {
    require(Phase.CARD);
    return new TelegramEditSession(chat, id, messageId, revision, null, null, Phase.MENU, deadline);
  }

  TelegramEditSession chooseFields() {
    require(Phase.MENU);
    return new TelegramEditSession(chat, id, anchor, before, null, null, Phase.FIELD, expires);
  }

  TelegramEditSession confirmDeletion() {
    require(Phase.MENU);
    return new TelegramEditSession(chat, id, anchor, before, null, null, Phase.DELETE, expires);
  }

  TelegramEditSession requestInput(TransactionField selected) {
    require(Phase.FIELD);
    return new TelegramEditSession(
        chat, id, anchor, before, Objects.requireNonNull(selected), null, Phase.INPUT, expires);
  }

  TelegramEditSession retryInput() {
    if (phase != Phase.INPUT && phase != Phase.PREVIEW) {
      throw new IllegalArgumentException("No editable field selected");
    }
    return new TelegramEditSession(chat, id, anchor, before, field, null, Phase.INPUT, expires);
  }

  TelegramEditSession preview(TransactionData data, int messageId) {
    require(Phase.INPUT);
    return new TelegramEditSession(
        chat, id, messageId, before, field, Objects.requireNonNull(data), Phase.PREVIEW, expires);
  }

  boolean expired(Instant now) {
    return !expires.isAfter(now);
  }

  private void require(Phase expected) {
    if (phase != expected)
      throw new IllegalArgumentException("Invalid transaction edit transition");
  }
}
