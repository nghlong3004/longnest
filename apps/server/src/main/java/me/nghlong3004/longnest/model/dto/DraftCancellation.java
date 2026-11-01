package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.nghlong3004.longnest.model.enums.DraftStatus;

@Schema(description = "Committed cancellation result for a command or inline button")
public record DraftCancellation(
    @Schema(description = "Resulting state; null when no draft was found", nullable = true)
        DraftStatus status,
    @Schema(
            description = "Confirmation identifying the cancelled draft",
            example = "Em hủy khoản “Ăn trưa” rồi nhé.")
        String reply,
    @Schema(description = "True only when this operation changed PENDING to CANCELLED")
        boolean changed,
    @Schema(
            description = "Original user message to reply to when notifying cancellation",
            nullable = true)
        String originalMessageId) {}
