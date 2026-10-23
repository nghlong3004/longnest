package me.nghlong3004.longnest.ledger.model;

import java.time.Instant;

/** Host-only authenticated context. Never deserialize this from transaction/model input. */
public record HostContext(
    String principal, String operationId, Instant businessReference, Instant messageAt,
    String interactionScope) {
  public HostContext(String principal, String operationId, Instant businessReference,
      Instant messageAt) {
    this(principal, operationId, businessReference, messageAt, null);
  }
}
