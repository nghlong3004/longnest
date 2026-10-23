package me.nghlong3004.longnest.domain;

import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Category {
  FOOD(TransactionType.EXPENSE),
  GROCERIES(TransactionType.EXPENSE),
  TRANSPORT(TransactionType.EXPENSE),
  RENT(TransactionType.EXPENSE),
  SALARY(TransactionType.INCOME),
  OTHER_INCOME(TransactionType.INCOME),
  UNCATEGORIZED(null);

  private final TransactionType type;

  public boolean supports(TransactionType candidate) {
    Objects.requireNonNull(candidate, "type");
    return this == UNCATEGORIZED || type == candidate;
  }
}
