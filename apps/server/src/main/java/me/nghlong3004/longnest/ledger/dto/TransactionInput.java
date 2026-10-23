package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;

@Schema(description = "Create data only; host identity, IDs and confirmation are not writable")
public record TransactionInput(
    @NotNull @Schema(description = "Income or expense", example = "EXPENSE") TransactionType type,
    @NotBlank @Pattern(regexp = "[1-9][0-9]{0,12}")
        @Schema(description = "Canonical positive whole-dong amount <= 10^12", example = "45000")
        String amountMinor,
    @NotNull @Pattern(regexp = "VND")
        @Schema(description = "Only VND is supported", example = "VND") String currency,
    @NotNull @Schema(description = "Absent uses frozen host business date; explicit null invalid",
        example = "2026-10-20") Optional<String> transactionDate,
    @NotNull @Schema(description = "Absent means UNCATEGORIZED", example = "FOOD")
        Optional<Category> category,
    @NotNull @Schema(description = "Absent/empty is no note; single-line NFC, max 256 code points",
        example = "ăn trưa") Optional<String> note) {}
