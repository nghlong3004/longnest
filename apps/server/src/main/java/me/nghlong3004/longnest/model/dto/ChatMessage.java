package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import me.nghlong3004.longnest.model.enums.Channel;

@Schema(
    description = "Incoming text and trusted metadata supplied by an authorized channel adapter")
public record ChatMessage(
    @NotNull @Schema(description = "Source channel", example = "TELEGRAM") Channel channel,
    @NotBlank @Schema(description = "Sender ID on the source channel", example = "123")
        String externalUserId,
    @NotBlank @Schema(description = "Source conversation ID", example = "123") String chatId,
    @NotBlank @Schema(description = "Original message ID within the conversation", example = "42")
        String messageId,
    @NotNull
        @Schema(description = "Original message timestamp in UTC", example = "2026-11-01T05:00:00Z")
        Instant sentAt,
    @NotBlank @Schema(description = "Literal user text", example = "Ăn trưa 45k") String text,
    @Schema(description = "Message being replied to on the source channel", nullable = true)
        String replyToMessageId) {

  public ChatMessage(
      Channel channel,
      String externalUserId,
      String chatId,
      String messageId,
      Instant sentAt,
      String text) {
    this(channel, externalUserId, chatId, messageId, sentAt, text, null);
  }
}
