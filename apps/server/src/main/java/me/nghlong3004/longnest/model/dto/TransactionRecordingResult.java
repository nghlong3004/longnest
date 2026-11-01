package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Atomic decision: a committed transaction or the existing draft for this source")
public record TransactionRecordingResult(
    @Schema(
            description = "Committed financial details when a transaction was recorded",
            nullable = true)
        TransactionData recorded,
    @Schema(
            description = "Existing conversation state when this source continues a draft",
            nullable = true)
        DraftContext draft) {

  public TransactionRecordingResult {
    if ((recorded == null) == (draft == null)) {
      throw new IllegalArgumentException("Exactly one recording result is required");
    }
  }
}
