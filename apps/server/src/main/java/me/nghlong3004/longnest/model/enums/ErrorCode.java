package me.nghlong3004.longnest.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Stable application error codes and their localized message keys. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  /** The chatbot provider could not be reached or rejected the request. */
  CHATBOT_UNAVAILABLE("error.chatbot.unavailable"),
  /** The chatbot returned no usable answer. */
  CHATBOT_EMPTY_RESPONSE("error.chatbot.empty-response"),
  /** The model output could not be parsed or violated the transaction contract. */
  CHATBOT_INVALID_RESPONSE("error.chatbot.invalid-response"),
  /** A transaction could not be saved successfully. */
  TRANSACTION_SAVE_FAILED("error.transaction.save-failed"),
  /** An unexpected application failure occurred. */
  INTERNAL_ERROR("error.internal");

  private final String messageKey;
}
