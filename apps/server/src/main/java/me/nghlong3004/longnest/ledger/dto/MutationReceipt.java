package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Committed create outcome; replay is original, not current state")
public record MutationReceipt(
    @NotNull @Schema(description = "Host operation identity", example = "event-42")
        String operationId,
    @NotNull @Schema(description = "Operation kind", example = "create") String operation,
    @Schema(description = "Whether the original operation changed the ledger", example = "true")
        boolean changed,
    @Schema(description = "True when recovering an original committed outcome", example = "false")
        boolean replayed,
    @NotNull @Valid @Schema(description = "Immutable original transaction snapshot")
        TransactionView transaction,
    @NotNull @Schema(description = "Original ledger generation as a decimal string", example = "1")
        String generation) {}
