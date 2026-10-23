package me.nghlong3004.longnest.domain;

import java.util.Objects;

public enum RangeFilter {
  ALL,
  INCOME,
  EXPENSE,
  FOOD;

  public boolean includes(TransactionType type) {
    Objects.requireNonNull(type, "type");
    return this == ALL || (this == FOOD ? type == TransactionType.EXPENSE
        : name().equals(type.name()));
  }

  public boolean includes(TransactionType type, Category category) {
    Objects.requireNonNull(category, "category");
    return includes(type) && (this != FOOD || category == Category.FOOD);
  }
}
