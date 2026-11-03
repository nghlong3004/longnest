package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Internal service decision: exactly one of a committed transaction or an existing draft for"
            + " this source; not a new public endpoint")
public record TransactionRecordingResult(
    @Schema(
            description = "Committed financial details when a transaction was recorded",
            example =
                "{\"type\":\"EXPENSE\",\"amount\":45000,\"category\":\"FOOD\",\"description\":\"Ăn"
                    + " trưa\",\"location\":null,\"date\":\"2026-11-01\",\"timeOfDay\":\"NOON\"}",
            nullable = true)
        TransactionData recorded,
    @Schema(
            description = "Existing conversation state when this source continues a draft",
            example =
                "{\"id\":\"4cf4f284-41c9-4f87-9c30-56203812cfe9\",\"status\":\"PENDING\",\"transaction\":{\"type\":\"EXPENSE\",\"amount\":null,\"category\":null,\"description\":\"Ăn"
                    + " trưa\",\"location\":null,\"date\":\"2026-11-01\",\"timeOfDay\":null},\"originalText\":\"Ăn"
                    + " trưa\",\"questionText\":\"Cho em xin số tiền của khoản này"
                    + " nhé.\",\"questionMessageId\":null,\"originalSentAt\":\"2026-11-01T05:00:00Z\",\"originalMessageId\":\"42\",\"version\":0,\"replayed\":true}",
            nullable = true)
        DraftContext draft) {

  public TransactionRecordingResult {
    if ((recorded == null) == (draft == null)) {
      throw new IllegalArgumentException("Exactly one recording result is required");
    }
  }
}
