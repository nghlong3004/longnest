package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;

@Schema(description = "Owned immutable view; deleted state supports a future Restore control")
public record TransactionView(
    @NotNull @Pattern(regexp = "[1-9][0-9]*")
        @Schema(description = "Stable positive decimal ID", example = "42") String id,
    @NotNull @Pattern(regexp = "[1-9][0-9]*")
        @Schema(description = "Business revision, distinct from ORM row version", example = "1")
        String revision,
    @NotNull @Schema(description = "Income or expense", example = "EXPENSE") TransactionType type,
    @NotNull @Schema(description = "Positive whole-dong decimal string", example = "45000")
        String amountMinor,
    @NotNull @Schema(description = "Currency", example = "VND") String currency,
    @NotNull @Schema(description = "Compatible category", example = "FOOD") Category category,
    @NotNull @Schema(description = "Normalized note, possibly empty", example = "ăn trưa")
        String note,
    @NotNull @Schema(description = "Financial date, not creation date", example = "2026-10-20")
        LocalDate transactionDate,
    @NotNull @Schema(description = "Immutable server persistence instant at microsecond precision",
        example = "2026-10-20T05:00:00Z") Instant createdAt,
    @NotNull @Schema(description = "Optional authenticated source instant, microsecond precision",
        example = "2026-10-20T04:59:59Z") Optional<Instant> messageAt,
    @NotNull @Schema(description = "ACTIVE or DELETED; no deletion method is implemented here",
        example = "ACTIVE") State state) {
  public enum State { ACTIVE, DELETED }
}
