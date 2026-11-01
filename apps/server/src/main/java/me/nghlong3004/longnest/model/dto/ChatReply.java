package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "User-facing reply and optional draft awaiting clarification")
public record ChatReply(
    @NotBlank @Schema(description = "Plain text to send", example = "Anh chi bao nhiêu?")
        String text,
    @Schema(description = "Pending draft to attach to the sent question", nullable = true)
        UUID draftId) {

  public static ChatReply text(String text) {
    return new ChatReply(text, null);
  }
}
