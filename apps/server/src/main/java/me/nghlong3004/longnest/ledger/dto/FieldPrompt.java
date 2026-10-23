package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import me.nghlong3004.longnest.ledger.model.EditField;

@Schema(description = "Persisted owner/target/revision/field prompt awaiting a bound host reply")
public record FieldPrompt(
    @NotNull String promptId,
    @NotNull EditField field,
    @NotNull @Valid TransactionView before,
    @NotNull @Valid TransactionView current,
    @NotNull Instant expiresAt,
    Long displayMessage) {}
