package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import me.nghlong3004.longnest.model.enums.DraftStatus;

@Schema(
    description =
        "Internal detached, owner-scoped service snapshot for interpreting a draft reply; not a new"
            + " public endpoint")
public record DraftContext(
    @Schema(
            description = "Draft ID",
            example = "4cf4f284-41c9-4f87-9c30-56203812cfe9",
            format = "uuid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
    @Schema(
            description = "Current lifecycle state",
            example = "PENDING",
            requiredMode = Schema.RequiredMode.REQUIRED)
        DraftStatus status,
    @Schema(
            description =
                "Known details, committed details after confirmation, or an empty details record"
                    + " after permanent deletion",
            example =
                "{\"type\":\"EXPENSE\",\"amount\":null,\"category\":null,\"description\":\"Ăn"
                    + " trưa\",\"location\":null,\"date\":\"2026-11-01\",\"timeOfDay\":null}",
            requiredMode = Schema.RequiredMode.REQUIRED)
        TransactionData transaction,
    @Schema(
            description =
                "Original user message; replaced by a deletion marker after permanent deletion",
            example = "Ăn trưa",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String originalText,
    @Schema(
            description = "Latest clarification question; null before a question or after deletion",
            example = "Cho em xin số tiền của khoản này nhé.",
            nullable = true)
        String questionText,
    @Schema(
            description = "Last registered sent question ID; null until delivery is registered",
            example = "43",
            nullable = true)
        String questionMessageId,
    @Schema(
            description = "Original source timestamp",
            example = "2026-11-01T16:59:00Z",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Instant originalSentAt,
    @Schema(
            description = "Original source message ID",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String originalMessageId,
    @Schema(
            description = "Version observed before calling the model",
            example = "1",
            minimum = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long version,
    @Schema(
            description = "Whether returning existing state without applying a new operation",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean replayed) {}
