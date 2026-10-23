package me.nghlong3004.longnest.domain;

import java.util.Objects;

public enum RangeFilter {
  ALL,
  INCOME,
  EXPENSE;

  public boolean includes(TransactionType type) {
    Objects.requireNonNull(type, "type");
    return this == ALL || name().equals(type.name());
  }
}
