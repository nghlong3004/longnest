package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;

@Schema(description = "Transaction details extracted from a message; unknown values remain null")
public record TransactionData(
    @Schema(description = "Income or expense, when clear", example = "EXPENSE", nullable = true)
        TransactionType type,
    @Positive
        @Digits(integer = 17, fraction = 2)
        @Schema(description = "Positive amount in VND", example = "45000", nullable = true)
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
            description = "An unambiguous explicit transaction date; otherwise null",
            example = "2026-11-01",
            nullable = true)
        LocalDate date,
    @Schema(description = "Part of the day indicated by the user", example = "NOON", nullable = true)
        TimeOfDay timeOfDay) {}
