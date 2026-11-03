package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;

@Schema(
    description =
        "Owner-scoped transaction details, including validated edit previews for the same stable"
            + " ID")
public record TransactionResponse(
    @Schema(
            description = "Stable stored transaction ID",
            example = "4cf4f284-41c9-4f87-9c30-56203812cfe9",
            format = "uuid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
    @Schema(
            description = "Income or expense",
            example = "EXPENSE",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionType type,
    @Schema(
            description = "Positive VND amount, up to 17 integer and 2 fractional digits",
            example = "45000.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,
    @Schema(
            description = "Expense category; null for income or unknown purpose",
            example = "FOOD",
            nullable = true)
        TransactionCategory category,
    @Schema(description = "Transaction purpose", example = "Ăn trưa", nullable = true)
        String description,
    @Schema(description = "Transaction location", example = "Quán cơm", nullable = true)
        String location,
    @Schema(
            description = "Transaction date",
            example = "2026-11-01",
            format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,
    @Schema(description = "Part of day, if known", example = "NOON", nullable = true)
        TimeOfDay timeOfDay) {}
