package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.nghlong3004.longnest.model.enums.DraftStatus;

@Schema(
    description =
        "Internal service result after resolving a cancellation command/button; not a new public"
            + " endpoint")
public record DraftCancellation(
    @Schema(
            description = "Resulting state; null when no draft was found",
            example = "CANCELLED",
            nullable = true)
        DraftStatus status,
    @Schema(
            description =
                "Truthful outcome: cancellation, no draft, already recorded or already deleted",
            example = "Em hủy khoản “Ăn trưa” rồi nhé.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String reply,
    @Schema(
            description = "True only when this operation changed PENDING to CANCELLED",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean changed,
    @Schema(
            description = "Original user message to reply to when notifying cancellation",
            example = "42",
            nullable = true)
        String originalMessageId) {}
