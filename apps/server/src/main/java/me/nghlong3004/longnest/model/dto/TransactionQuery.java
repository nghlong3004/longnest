package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.model.enums.TransactionType;

@Schema(description = "Read-only income/expense filter proposed by the model")
public record TransactionQuery(
    @NotNull
        @Schema(
            description = "Income or expense to view",
            example = "EXPENSE",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionType type,
    @NotNull
        @Schema(
            description = "Calendar period; backend resolves relative dates",
            example = "THIS_MONTH",
            requiredMode = Schema.RequiredMode.REQUIRED)
        QueryPeriod period,
    @Schema(
            description = "Required start for CUSTOM or SINCE_DATE; null for relative periods",
            example = "2026-11-01",
            format = "date",
            nullable = true)
        LocalDate from,
    @Schema(
            description = "Required inclusive end for CUSTOM; null for other periods",
            example = "2026-11-02",
            format = "date",
            nullable = true)
        LocalDate to) {}
