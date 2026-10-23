package me.nghlong3004.longnest.ledger.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.model.CreateIntent;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntryValues {
  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_type", nullable = false, length = 16)
  private TransactionType type;

  @Column(name = "amount_minor", nullable = false)
  private long amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Category category;

  @Column(nullable = false, length = 1024)
  private String note;

  @Column(name = "transaction_date", nullable = false)
  private LocalDate transactionDate;

  EntryValues(CreateIntent intent) {
    var draft = intent.draft();
    type = draft.type();
    amount = draft.amount().amountMinor();
    category = draft.category();
    note = draft.note().value();
    transactionDate = intent.transactionDate();
  }

  CreateIntent intent() {
    return new CreateIntent(new TransactionDraft(type, new Money(amount), category, new Note(note)),
        transactionDate);
  }
}
