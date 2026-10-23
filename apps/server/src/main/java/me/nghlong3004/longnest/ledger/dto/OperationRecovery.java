package me.nghlong3004.longnest.ledger.dto;

/** Original durable outcome of one host event; never model-generated success or proof. */
public record OperationRecovery(MutationReceipt receipt, ConfirmationRequired preview) {
  public OperationRecovery {
    if ((receipt == null) == (preview == null)) {
      throw new IllegalArgumentException("Exactly one recovered outcome required");
    }
  }
}
