package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Last displayed date/id in descending keyset order")
public record TransactionCursor(
    @NotNull
        @Schema(
            description = "Last displayed transaction date",
            example = "2026-11-01",
            format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,
    @NotNull
        @Schema(
            description = "Last displayed transaction ID",
            example = "4cf4f284-41c9-4f87-9c30-56203812cfe9",
            format = "uuid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id) {}
