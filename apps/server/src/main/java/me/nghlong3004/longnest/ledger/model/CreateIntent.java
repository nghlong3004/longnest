package me.nghlong3004.longnest.ledger.model;

import java.time.LocalDate;
import java.util.Objects;
import me.nghlong3004.longnest.domain.TransactionDraft;

/** Canonical, frozen business input; never a managed entity. */
public record CreateIntent(TransactionDraft draft, LocalDate transactionDate) {
  public CreateIntent {
    Objects.requireNonNull(draft);
    if (transactionDate == null || transactionDate.isBefore(LocalDate.of(1900, 1, 1))
        || transactionDate.isAfter(LocalDate.of(9999, 12, 31))) {
      throw new LedgerException(LedgerException.Code.INVALID_DATE);
    }
  }
}
