package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "User-facing reply and optional draft awaiting clarification")
public record ChatReply(
    @NotBlank
        @Schema(
            description = "Nonblank plain text to send, without markup parsing",
            example = "Cho em xin số tiền của khoản này nhé.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String text,
    @Schema(
            description =
                "Pending draft to attach to the sent question; mutually exclusive with"
                    + " transactions",
            example = "4cf4f284-41c9-4f87-9c30-56203812cfe9",
            format = "uuid",
            nullable = true)
        UUID draftId,
    @Schema(
            description =
                "Query page to display as separate transaction messages; mutually exclusive with"
                    + " draftId",
            example =
                "{\"filter\":{\"type\":\"EXPENSE\",\"from\":\"2026-11-01\",\"to\":\"2026-11-02\"},\"items\":[],\"totals\":{\"amount\":0,\"count\":0},\"nextCursor\":null}",
            nullable = true)
        TransactionPage transactions) {

  public ChatReply(String text, UUID draftId) {
    this(text, draftId, null);
  }

  public ChatReply {
    if (draftId != null && transactions != null) {
      throw new IllegalArgumentException("A query page cannot also be a draft question");
    }
  }

  public static ChatReply text(String text) {
    return new ChatReply(text, null);
  }
}
