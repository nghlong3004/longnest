package me.nghlong3004.longnest.integration.telegram;

import java.time.Instant;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/** Maps only metadata already authenticated by the Telegram message adapter. */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TelegramMessageMapper {
  @Mapping(target = "channel", constant = "TELEGRAM")
  @Mapping(target = "externalUserId", source = "sender.id")
  @Mapping(target = "chatId", source = "message.chatId")
  @Mapping(target = "messageId", source = "message.messageId")
  @Mapping(target = "sentAt", source = "message.date")
  @Mapping(target = "text", source = "text")
  @Mapping(target = "replyToMessageId", source = "message.replyToMessage.messageId")
  @Mapping(target = "displayName", expression = "java(displayName(sender))")
  ChatMessage toChatMessage(Message message, User sender, String text);

  default Instant sentAt(Integer epochSeconds) {
    return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
  }

  default String displayName(User sender) {
    return ((sender.getFirstName() == null ? "" : sender.getFirstName())
            + " "
            + (sender.getLastName() == null ? "" : sender.getLastName()))
        .strip();
  }
}
