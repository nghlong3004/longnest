package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Exact aggregates for the complete owner/type/date filter")
public record TransactionTotals(
    @Schema(
            description = "Sum over all matching transactions",
            example = "150000.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,
    @Schema(
            description = "Number of matching transactions",
            example = "8",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long count) {}
