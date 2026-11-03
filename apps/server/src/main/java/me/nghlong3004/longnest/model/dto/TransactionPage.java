package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "One bounded transaction page plus aggregates for the whole filter")
public record TransactionPage(
    @Schema(
            description = "Resolved inclusive filter",
            example = "{\"type\":\"EXPENSE\",\"from\":\"2026-11-01\",\"to\":\"2026-11-02\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionFilter filter,
    @Schema(
            description =
                "At most five entries ordered by date descending, then ID descending; empty if none"
                    + " match",
            example = "[]",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<TransactionResponse> items,
    @Schema(
            description = "Whole-filter totals, not just this page",
            example = "{\"amount\":150000,\"count\":8}",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionTotals totals,
    @Schema(
            description = "Last displayed key when more rows exist; otherwise null",
            example = "{\"date\":\"2026-11-01\",\"id\":\"4cf4f284-41c9-4f87-9c30-56203812cfe9\"}",
            nullable = true)
        TransactionCursor nextCursor) {

  public TransactionPage {
    items = List.copyOf(items);
  }
}
