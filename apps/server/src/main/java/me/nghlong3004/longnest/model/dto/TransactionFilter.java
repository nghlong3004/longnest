package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import me.nghlong3004.longnest.model.enums.TransactionType;

@Schema(description = "Validated filter with both calendar dates resolved inclusively")
public record TransactionFilter(
    @Schema(
            description = "Income or expense",
            example = "EXPENSE",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionType type,
    @Schema(
            description = "First included date",
            example = "2026-11-01",
            format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate from,
    @Schema(
            description = "Last included date",
            example = "2026-11-02",
            format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate to) {}
