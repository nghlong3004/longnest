package me.nghlong3004.longnest.ledger.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** Immutable global host-event binding for callback action/verb replay. */
@Entity
@Table(name = "ledger_control_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ControlEvent {
  @EmbeddedId
  private InboxId id;

  @Column(nullable = false, length = 64)
  @Getter
  @Accessors(fluent = true)
  private String fingerprint;

  @Column(name = "control_id", nullable = false, length = 32)
  private String controlId;

  public static ControlEvent bound(String owner, String operationId,
      String fingerprint, String controlId) {
    if (owner == null || owner.isBlank() || owner.length() > 200
        || operationId == null || operationId.isBlank() || operationId.length() > 200
        || fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")
        || controlId == null || !controlId.matches("[0-9a-f]{32}")) {
      throw new IllegalArgumentException("Invalid control event binding");
    }
    return new ControlEvent(new InboxId(owner, operationId), fingerprint, controlId);
  }

}
