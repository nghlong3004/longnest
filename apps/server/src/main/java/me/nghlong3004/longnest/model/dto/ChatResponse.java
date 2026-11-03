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
            description =
                "Ready transaction, clarification question, read-only query, or ordinary chat",
            example = "READY",
            requiredMode = Schema.RequiredMode.REQUIRED)
        ChatAction action,
    @Valid
        @Schema(
            description =
                "Known transaction details; required for READY, optional for clarification, null"
                    + " for CHAT and QUERY",
            example =
                "{\"type\":\"EXPENSE\",\"amount\":45000,\"category\":\"FOOD\",\"description\":\"Ăn"
                    + " trưa\",\"location\":null,\"date\":\"2026-11-01\",\"timeOfDay\":\"NOON\"}",
            nullable = true)
        TransactionData transaction,
    @NotBlank
        @Schema(
            description = "One short Vietnamese sentence for the user, without claiming a save",
            example = "Khoản chi 45.000đ cho ăn trưa — chưa lưu.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String reply,
    @Valid
        @Schema(
            description = "Read-only query details; required for QUERY, null for other actions",
            example = "{\"type\":\"EXPENSE\",\"period\":\"THIS_MONTH\",\"from\":null,\"to\":null}",
            nullable = true)
        TransactionQuery query) {

  public ChatResponse(ChatAction action, TransactionData transaction, String reply) {
    this(action, transaction, reply, null);
  }
}
