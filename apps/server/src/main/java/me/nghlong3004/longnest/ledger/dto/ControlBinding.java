package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlKind;

@Schema(description = "Read-only host metadata for constructing a trusted callback proof")
public record ControlBinding(
    @NotNull String id,
    @NotNull ControlKind kind,
    long targetId,
    long expectedRevision,
    Long displayMessage) {}
