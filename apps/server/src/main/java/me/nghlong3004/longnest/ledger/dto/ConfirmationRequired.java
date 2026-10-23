package me.nghlong3004.longnest.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(description = "Immutable application-owned preview; no financial change yet")
public record ConfirmationRequired(
    @NotNull @Schema(description = "Opaque action identity", example = "xYz123") String actionId,
    @NotNull @Schema(description = "UPDATE, DELETE or RESTORE") Kind kind,
    @NotNull @Valid @Schema(description = "Original current entry") TransactionView before,
    @NotNull @Valid @Schema(description = "Validated result if completed") TransactionView after,
    @NotNull @Schema(description = "Original expected business revision", example = "1")
        String expectedRevision,
    @NotNull @Schema(description = "Ten-minute pending-action expiry") Instant expiresAt,
    @Schema(description = "Bound bot message ID, null until send succeeds") Long displayMessage) {
  public enum Kind { UPDATE, DELETE, RESTORE }
}
