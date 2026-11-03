package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;

@Schema(
    description =
        "Transaction details for extraction, draft merging or committed snapshots. Unknown"
            + " extracted fields remain null; null retains an existing draft field. Edit previews"
            + " are complete replacements, with clearing handled by the mutation service.")
public record TransactionData(
    @Schema(description = "Income or expense, when clear", example = "EXPENSE", nullable = true)
        TransactionType type,
    @Positive
        @Digits(integer = 17, fraction = 2)
        @Schema(
            description =
                "Positive amount in VND, up to 17 integer and 2 fractional digits; null when"
                    + " unknown",
            example = "45000.00",
            minimum = "0",
            exclusiveMinimum = true,
            nullable = true)
        BigDecimal amount,
    @Schema(
            description = "Expense category; null for income or an unknown purpose",
            example = "FOOD",
            nullable = true)
        TransactionCategory category,
    @Schema(description = "Purpose stated by the user", example = "Ăn trưa", nullable = true)
        String description,
    @Schema(description = "Location stated by the user", example = "Quán cơm", nullable = true)
        String location,
    @Schema(
            description =
                "Resolved calendar date when unambiguous; null when unknown, retaining an existing"
                    + " draft date during merging",
            example = "2026-11-01",
            format = "date",
            nullable = true)
        LocalDate date,
    @Schema(
            description = "Part of the day indicated by the user",
            example = "NOON",
            nullable = true)
        TimeOfDay timeOfDay) {}
