package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;

@Schema(description = "Present fields replace one owned transaction's values after validation")
public record TransactionPatch(
    @NotNull @Schema(description = "Optional canonical whole-dong amount", example = "40000")
        Optional<String> amountMinor,
    @NotNull @Schema(description = "Optional income or expense type", example = "EXPENSE")
        Optional<TransactionType> type,
    @NotNull @Schema(description = "Optional compatible category", example = "FOOD")
        Optional<Category> category,
    @NotNull @Schema(description = "Optional note; present empty string clears it", example = "lunch 2")
        Optional<String> note,
    @NotNull @Schema(description = "Optional strict financial date", example = "2026-10-19")
        Optional<String> transactionDate) {
  public TransactionPatch {
    if (amountMinor == null || type == null || category == null || note == null
        || transactionDate == null) {
      throw new IllegalArgumentException("Patch fields must be explicit optionals");
    }
  }
}
