package me.nghlong3004.longnest.ledger.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;

/** Frozen intent plus original receipt; no raw message archive or current-entry replay lookup. */
@Entity
@Table(name = "ledger_create_inbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateInbox {
  @EmbeddedId
  private InboxId id;

  @Column(nullable = false, length = 64)
  private String fingerprint;

  @Embedded
  private EntryValues values;

  @Column(name = "business_reference", nullable = false)
  private Instant businessReference;

  @Column(name = "message_at")
  private Instant messageAt;

  @Column(name = "receipt_entry_id")
  private Long receiptEntryId;

  @Column(name = "receipt_generation")
  private Long receiptGeneration;

  @Column(name = "receipt_created_at")
  private Instant receiptCreatedAt;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

  public CreateInbox(LedgerStore.Inbox inbox) {
    if (inbox.receipt().isPresent()) {
      throw new IllegalArgumentException("Only uncompleted intent may be ingested");
    }
    id = new InboxId(inbox.owner(), inbox.operationId());
    fingerprint = inbox.fingerprint();
    values = new EntryValues(inbox.intent());
    businessReference = inbox.businessReference();
    messageAt = inbox.messageAt().orElse(null);
  }

  public void complete(LedgerStore.Receipt receipt) {
    if (receiptEntryId != null) {
      throw new IllegalStateException("A completed receipt cannot be replaced");
    }
    receiptEntryId = receipt.entryId();
    receiptGeneration = receipt.generation();
    receiptCreatedAt = receipt.createdAt();
  }

  public LedgerStore.Inbox snapshot() {
    Optional<LedgerStore.Receipt> receipt = receiptEntryId == null ? Optional.empty()
        : Optional.of(new LedgerStore.Receipt(receiptEntryId, receiptGeneration, receiptCreatedAt));
    return new LedgerStore.Inbox(id.getOwner(), id.getOperationId(), fingerprint, values.intent(),
        businessReference, Optional.ofNullable(messageAt), receipt);
  }
}
