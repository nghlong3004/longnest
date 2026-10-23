package me.nghlong3004.longnest.ledger.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;

@Entity
@Table(name = "ledger_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ledger_entry_ids")
  @SequenceGenerator(
      name = "ledger_entry_ids", sequenceName = "ledger_entry_id_seq", allocationSize = 1)
  private Long id;

  @Column(name = "owner_id", nullable = false, length = 200)
  private String owner;

  @Embedded
  private EntryValues values;

  @Column(name = "business_revision", nullable = false)
  private long revision;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "message_at")
  private Instant messageAt;

  @Column(nullable = false)
  private boolean deleted;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

  public LedgerEntry(String owner, CreateIntent intent, Instant createdAt,
      Optional<Instant> messageAt) {
    this.owner = owner;
    this.values = new EntryValues(intent);
    this.revision = 1;
    this.createdAt = createdAt;
    this.messageAt = messageAt.orElse(null);
  }

  public LedgerStore.Entry snapshot() {
    return new LedgerStore.Entry(owner, id, revision, values.intent(), createdAt,
        Optional.ofNullable(messageAt), deleted);
  }

  public void mutate(long expectedRevision, CreateIntent after, boolean nextDeleted) {
    if (revision != expectedRevision || revision == Long.MAX_VALUE) {
      throw new LedgerException(LedgerException.Code.REVISION_CONFLICT);
    }
    values = new EntryValues(after);
    deleted = nextDeleted;
    revision++;
  }
}
