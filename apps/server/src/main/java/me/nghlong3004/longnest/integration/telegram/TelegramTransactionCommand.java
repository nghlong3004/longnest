package me.nghlong3004.longnest.integration.telegram;

import me.nghlong3004.longnest.model.enums.TransactionField;

/** Decoded callback data is still subject to owner, anchor, phase and version checks. */
record TelegramTransactionCommand(String key, Operation operation, TransactionField field) {
  enum Operation {
    MENU("m"),
    EDIT("e"),
    DELETE("d"),
    CANCEL("c"),
    SAVE("save"),
    CONFIRM_DELETE("yes"),
    FIELD(null);
    private final String value;

    Operation(String value) {
      this.value = value;
    }

    String value() {
      return value;
    }
  }

  static TelegramTransactionCommand decode(String data) {
    if (data == null || !data.matches("tx:[0-9a-f]{32}:[A-Za-z]+")) {
      throw new IllegalArgumentException("Invalid transaction callback");
    }
    int separator = data.lastIndexOf(':');
    var key = data.substring(0, separator);
    var value = data.substring(separator + 1);
    for (var operation : Operation.values()) {
      if (value.equals(operation.value))
        return new TelegramTransactionCommand(key, operation, null);
    }
    return new TelegramTransactionCommand(key, Operation.FIELD, TransactionField.fromValue(value));
  }
}
