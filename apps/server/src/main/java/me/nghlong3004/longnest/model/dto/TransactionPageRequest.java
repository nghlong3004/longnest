package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Decoded, untrusted pagination request; owner always comes from the adapter")
public record TransactionPageRequest(
    @Schema(
            description = "Fixed CUSTOM dates and transaction type",
            example =
                "{\"type\":\"EXPENSE\",\"period\":\"CUSTOM\",\"from\":\"2026-11-01\",\"to\":\"2026-11-02\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionQuery query,
    @Schema(
            description = "Last displayed transaction key",
            example = "{\"date\":\"2026-11-01\",\"id\":\"4cf4f284-41c9-4f87-9c30-56203812cfe9\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionCursor cursor,
    @Schema(
            description = "Original user query message for native Telegram replies",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int messageId) {}
