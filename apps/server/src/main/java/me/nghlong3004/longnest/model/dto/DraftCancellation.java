package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.nghlong3004.longnest.model.enums.DraftStatus;

@Schema(description = "Committed cancellation result for a command or inline button")
public record DraftCancellation(
    @Schema(description = "Resulting state; null when no draft was found", nullable = true)
        DraftStatus status,
    @Schema(description = "Short notification for the user", example = "Đã hủy.") String reply) {}
