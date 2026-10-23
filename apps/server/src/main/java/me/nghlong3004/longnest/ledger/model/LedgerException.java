package me.nghlong3004.longnest.ledger.model;

import lombok.Getter;

@Getter
public final class LedgerException extends RuntimeException {
  public enum Code {
    INVALID_AMOUNT, INVALID_DATE, INVALID_RANGE, INVALID_FIELD, INVALID_STATE, NOT_FOUND_OR_FORBIDDEN,
    IDEMPOTENCY_CONFLICT, LIMIT_REACHED, INVALID_CURSOR, STALE_CURSOR,
    REVISION_CONFLICT, CONFIRMATION_REQUIRED, ACTION_EXPIRED, ACTION_CANCELED,
    ACTION_COMPLETED, INVALID_ACTION_PROOF,
    NOT_SAVED, OUTCOME_UNKNOWN
  }

  private final Code code;

  public LedgerException(Code code) {
    super(code.name());
    this.code = code;
  }
}
