package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Schema(description = "Owned recent or financial-date range page with opaque keyset cursor")
public record TransactionPage(
    @NotNull @Schema(description = "Query scope", example = "recent") String scope,
    @NotNull @Valid @Schema(description = "At most 10 active entries")
        List<TransactionView> entries,
    @Schema(description = "Fixed page size", example = "10") int pageSize,
    @NotNull @Schema(description = "Observed ledger generation", example = "12") String generation,
    @NotNull @Schema(description = "Opaque next token, absent on the last page")
        Optional<String> nextCursor,
    @NotNull @Valid @Schema(description = "Full-range summary, absent for recent")
        Optional<RangeSummary> summary) {
  public TransactionPage {
    entries = List.copyOf(entries);
    if (nextCursor == null || summary == null || entries.size() > 10 || pageSize != 10
        || generation == null || !generation.matches("0|[1-9][0-9]*")
        || !("recent".equals(scope) && summary.isEmpty()
            || "range".equals(scope) && summary.isPresent())) {
      throw new IllegalArgumentException("Invalid transaction page");
    }
    if (summary.isPresent() && !generation.equals(summary.orElseThrow().generation())) {
      throw new IllegalArgumentException("Page and summary generation differ");
    }
  }

  public TransactionPage(String scope, List<TransactionView> entries, int pageSize,
      String generation, Optional<String> nextCursor) {
    this(scope, entries, pageSize, generation, nextCursor, Optional.empty());
  }
}
