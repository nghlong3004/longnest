package me.nghlong3004.longnest.ledger.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxId implements Serializable {
  @Column(name = "owner_id", length = 200)
  private String owner;

  @Column(name = "operation_id", length = 200)
  private String operationId;
}
