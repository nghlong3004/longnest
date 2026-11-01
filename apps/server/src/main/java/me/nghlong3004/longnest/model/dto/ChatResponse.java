package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.nghlong3004.longnest.model.enums.ChatAction;

@Schema(description = "Structured interpretation proposed by the chatbot, before any persistence")
public record ChatResponse(
    @NotNull
        @Schema(
            description = "Ready transaction, clarification question, or ordinary chat",
            example = "READY",
            requiredMode = Schema.RequiredMode.REQUIRED)
        ChatAction action,
    @Valid
        @Schema(description = "Known transaction details; null for ordinary chat", nullable = true)
        TransactionData transaction,
    @NotBlank
        @Schema(
            description = "One short Vietnamese sentence for the user, without claiming a save",
            example = "Khoản chi 45.000đ cho ăn trưa — chưa lưu.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String reply) {}
