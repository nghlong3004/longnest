package me.nghlong3004.longnest.domain;

import java.util.Objects;

/** Validated input only: no owner, host date, persistent identity or saved outcome. */
public record TransactionDraft(TransactionType type, Money amount, Category category, Note note) {
  public TransactionDraft {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(note, "note");
    if (!category.supports(type)) {
      throw new IllegalArgumentException("Category is incompatible with transaction type");
    }
  }
}
