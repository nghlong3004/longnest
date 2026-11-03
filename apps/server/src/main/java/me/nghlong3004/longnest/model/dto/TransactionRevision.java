package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owner-scoped transaction snapshot for optimistic mutation confirmation")
public record TransactionRevision(
    @Schema(
            description = "Complete owner-scoped stored transaction",
            example =
                "{\"id\":\"4cf4f284-41c9-4f87-9c30-56203812cfe9\",\"type\":\"EXPENSE\",\"amount\":45000,\"category\":null,\"description\":null,\"location\":null,\"date\":\"2026-11-01\",\"timeOfDay\":null}",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionResponse transaction,
    @Schema(
            description = "Version that must still match at confirmation",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long version) {}
