package me.nghlong3004.longnest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import me.nghlong3004.longnest.model.enums.Channel;

@Schema(
    description = "Incoming text and trusted metadata supplied by an authorized channel adapter")
public record ChatMessage(
    @NotNull
        @Schema(
            description = "Source channel",
            example = "TELEGRAM",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Channel channel,
    @NotBlank
        @Schema(
            description = "Nonblank sender ID on the source channel",
            example = "123",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String externalUserId,
    @NotBlank
        @Schema(
            description = "Nonblank source conversation ID",
            example = "123",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String chatId,
    @NotBlank
        @Schema(
            description = "Nonblank original message ID within the conversation",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String messageId,
    @NotNull
        @Schema(
            description = "Original trusted message timestamp in UTC",
            example = "2026-11-01T05:00:00Z",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Instant sentAt,
    @NotBlank
        @Schema(
            description = "Nonblank literal user text",
            example = "Ăn trưa 45k",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String text,
    @Schema(
            description =
                "Message being replied to on the source channel; absent for a new message",
            example = "41",
            nullable = true)
        String replyToMessageId,
    @Schema(
            description =
                "Optional display label from trusted channel metadata, not identity or"
                    + " instructions; whitespace/control characters are sanitized and the label is"
                    + " limited to 80 Unicode code points",
            example = "Minh An",
            nullable = true)
        String displayName) {

  public ChatMessage {
    if (displayName != null) {
      var safe = new StringBuilder();
      displayName
          .codePoints()
          .forEach(cp -> {
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) safe.append(' ');
            else if (!Character.isISOControl(cp) && Character.getType(cp) != Character.FORMAT)
              safe.appendCodePoint(cp);
          });
      displayName = safe.toString().strip().replaceAll(" +", " ");
      if (displayName.codePointCount(0, displayName.length()) > 80)
        displayName = displayName.substring(0, displayName.offsetByCodePoints(0, 80));
      if (displayName.isBlank()) displayName = null;
    }
  }

  public ChatMessage(
      Channel channel,
      String externalUserId,
      String chatId,
      String messageId,
      Instant sentAt,
      String text,
      String replyToMessageId) {
    this(channel, externalUserId, chatId, messageId, sentAt, text, replyToMessageId, null);
  }

  public ChatMessage(
      Channel channel,
      String externalUserId,
      String chatId,
      String messageId,
      Instant sentAt,
      String text) {
    this(channel, externalUserId, chatId, messageId, sentAt, text, null, null);
  }
}
