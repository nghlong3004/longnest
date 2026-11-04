package me.nghlong3004.longnest.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Editable transaction fields and their stable external callback values. */
@Getter
@RequiredArgsConstructor
public enum TransactionField {
  AMOUNT("amount"),
  DATE("date"),
  TYPE("type"),
  CATEGORY("category"),
  DESCRIPTION("description"),
  LOCATION("location"),
  TIME_OF_DAY("timeOfDay");

  private final String value;

  public static TransactionField fromValue(String value) {
    for (var field : values()) {
      if (field.value.equals(value)) {
        return field;
      }
    }
    throw new IllegalArgumentException("Unknown transaction field: " + value);
  }
}
