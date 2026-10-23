package me.nghlong3004.longnest.ledger.model.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Control;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlKind;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlStatus;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Entry;

/** Durable display binding and immutable proposed outcome; never delete consumed controls. */
@Entity
@Table(name = "ledger_control")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class LedgerControl {
  @Id
  @Column(length = 32)
  private String id;

  @Column(name = "owner_id", nullable = false, length = 200)
  private String owner;

  @Column(name = "interaction_scope", nullable = false, length = 100)
  private String scope;

  @Column(name = "request_id", nullable = false, length = 200)
  private String requestId;

  @Column(name = "request_fingerprint", nullable = false, length = 64)
  private String requestFingerprint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ControlKind kind;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ControlStatus status;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private EditField field;

  @Column(name = "origin_id", length = 32)
  private String originId;

  @Column(name = "successor_id", length = 32)
  private String successorId;

  @Column(name = "target_id", nullable = false)
  private long targetId;

  @Column(name = "expected_revision", nullable = false)
  private long expectedRevision;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "message_at")
  private Instant messageAt;

  @Column(name = "before_deleted", nullable = false)
  private boolean beforeDeleted;

  @Column(name = "after_deleted", nullable = false)
  private boolean afterDeleted;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "type", column = @Column(name = "before_type", nullable = false, length = 16)),
      @AttributeOverride(name = "amount", column = @Column(name = "before_amount", nullable = false)),
      @AttributeOverride(name = "category", column = @Column(name = "before_category", nullable = false, length = 32)),
      @AttributeOverride(name = "note", column = @Column(name = "before_note", nullable = false, length = 1024)),
      @AttributeOverride(name = "transactionDate", column = @Column(name = "before_date", nullable = false))})
  private EntryValues beforeValues;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "type", column = @Column(name = "after_type", nullable = false, length = 16)),
      @AttributeOverride(name = "amount", column = @Column(name = "after_amount", nullable = false)),
      @AttributeOverride(name = "category", column = @Column(name = "after_category", nullable = false, length = 32)),
      @AttributeOverride(name = "note", column = @Column(name = "after_note", nullable = false, length = 1024)),
      @AttributeOverride(name = "transactionDate", column = @Column(name = "after_date", nullable = false))})
  private EntryValues afterValues;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "display_message")
  private Long displayMessage;

  @Column(name = "receipt_generation")
  private Long generation;

  @Column(name = "receipt_changed")
  private Boolean changed;

  @Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion;

  public static LedgerControl frozen(Control control) {
    Objects.requireNonNull(control, "control");
    var before = control.before();
    var after = control.after();
    if (control.status() != ControlStatus.PENDING || control.generation() != null
        || control.changed() != null || control.displayMessage() != null
        || before == null || after == null || before.id() <= 0 || before.revision() <= 0
        || !Objects.equals(control.owner(), before.owner())
        || !Objects.equals(before.owner(), after.owner())
        || before.id() != after.id() || before.revision() != after.revision()
        || !Objects.equals(before.createdAt(), after.createdAt())
        || !Objects.equals(before.messageAt(), after.messageAt())
        || (control.kind() == ControlKind.PROMPT) != (control.field() != null)) {
      throw new IllegalArgumentException("Invalid frozen ledger control");
    }
    return LedgerControl.builder()
        .id(control.id()).owner(control.owner()).scope(control.scope())
        .requestId(control.requestId()).requestFingerprint(control.requestFingerprint())
        .kind(control.kind()).status(control.status()).field(control.field())
        .originId(control.originId()).successorId(control.successorId())
        .targetId(before.id()).expectedRevision(before.revision())
        .createdAt(before.createdAt()).messageAt(before.messageAt().orElse(null))
        .beforeDeleted(before.deleted()).afterDeleted(after.deleted())
        .beforeValues(new EntryValues(before.intent()))
        .afterValues(new EntryValues(after.intent()))
        .expiresAt(control.expiresAt()).displayMessage(control.displayMessage())
        .generation(control.generation()).changed(control.changed()).build();
  }

  public Control snapshot() {
    var sourceTime = Optional.ofNullable(messageAt);
    return new Control(id, owner, scope, requestId, kind, status, field, originId, successorId,
        new Entry(owner, targetId, expectedRevision, beforeValues.intent(), createdAt,
            sourceTime, beforeDeleted),
        new Entry(owner, targetId, expectedRevision, afterValues.intent(), createdAt,
            sourceTime, afterDeleted), expiresAt, displayMessage, generation, changed,
        requestFingerprint);
  }

  public void bind(long message) {
    if (status != ControlStatus.PENDING || message <= 0
        || (displayMessage != null && displayMessage != message)) {
      throw new IllegalStateException("Control cannot bind display");
    }
    displayMessage = message;
  }

  public void finish(ControlStatus next, String successor, Long receiptGeneration,
      Boolean receiptChanged) {
    if (status != ControlStatus.PENDING) {
      throw new IllegalStateException("Control already consumed");
    }
    status = next;
    successorId = successor;
    generation = receiptGeneration;
    changed = receiptChanged;
  }
}
