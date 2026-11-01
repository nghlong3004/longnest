package me.nghlong3004.longnest.mapper;

import java.time.LocalDate;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.DraftContext;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.entity.DraftMessageLink;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.entity.TransactionDraft;
import me.nghlong3004.longnest.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DraftMapper {

  @Mapping(target = "user", source = "user")
  @Mapping(target = "date", source = "date")
  @Mapping(target = "channel", source = "message.channel")
  @Mapping(target = "chatId", source = "message.chatId")
  @Mapping(target = "messageId", source = "message.messageId")
  @Mapping(target = "originalText", source = "message.text")
  @Mapping(target = "originalSentAt", source = "message.sentAt")
  TransactionDraft toEntity(TransactionData data, ChatMessage message, User user, LocalDate date);

  TransactionData toData(TransactionDraft draft);

  TransactionData toData(Transaction transaction);

  @Mapping(target = "transaction", source = "data")
  @Mapping(target = "originalMessageId", source = "draft.messageId")
  DraftContext toContext(TransactionDraft draft, TransactionData data, boolean replayed);

  default DraftContext toContext(TransactionDraft draft, boolean replayed) {
    var data = draft.getTransaction() == null ? toData(draft) : toData(draft.getTransaction());
    return toContext(draft, data, replayed);
  }

  @Mapping(target = "draft", source = "draft")
  @Mapping(target = "channel", source = "message.channel")
  @Mapping(target = "chatId", source = "message.chatId")
  @Mapping(target = "messageId", source = "messageId")
  DraftMessageLink toLink(TransactionDraft draft, ChatMessage message, String messageId);

  @Mapping(target = "channel", source = "draft.channel")
  @Mapping(target = "externalUserId", source = "message.externalUserId")
  @Mapping(target = "chatId", source = "draft.chatId")
  @Mapping(target = "messageId", source = "draft.messageId")
  @Mapping(target = "sentAt", source = "draft.originalSentAt")
  @Mapping(target = "text", source = "draft.originalText")
  @Mapping(target = "replyToMessageId", ignore = true)
  ChatMessage toOriginalMessage(TransactionDraft draft, ChatMessage message);
}
