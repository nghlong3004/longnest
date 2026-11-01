package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import me.nghlong3004.longnest.model.enums.DraftStatus;

@Schema(description = "Detached, owner-scoped snapshot for interpreting a draft reply")
public record DraftContext(
    @Schema(description = "Draft ID") UUID id,
    @Schema(description = "Current lifecycle state", example = "PENDING") DraftStatus status,
    @Schema(description = "Known details, or committed details after confirmation")
        TransactionData transaction,
    @Schema(description = "Original user message", example = "Ăn trưa") String originalText,
    @Schema(description = "Current clarification question", nullable = true) String questionText,
    @Schema(description = "Current sent question ID", nullable = true) String questionMessageId,
    @Schema(description = "Original source timestamp", example = "2026-11-01T16:59:00Z")
        Instant originalSentAt,
    @Schema(description = "Original source message ID", example = "42") String originalMessageId,
    @Schema(description = "Version observed before calling the model", example = "1") long version,
    @Schema(description = "Whether returning existing state without applying a new operation")
        boolean replayed) {}
