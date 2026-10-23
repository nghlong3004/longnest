package me.nghlong3004.longnest.ledger.model;

import lombok.Getter;
import lombok.experimental.Accessors;

/** Opaque host capability; transaction/model DTOs cannot construct one. */
@Accessors(fluent = true)
public final class ControlProof {
  @Getter
  private final String scope;
  @Getter
  private final int messageId;
  @Getter
  private final String callbackId;
  private final byte[] signature;

  ControlProof(String scope, int messageId, String callbackId, byte[] signature) {
    this.scope = scope;
    this.messageId = messageId;
    this.callbackId = callbackId;
    this.signature = signature.clone();
  }

  byte[] signature() { return signature.clone(); }
}
