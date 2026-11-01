package me.nghlong3004.longnest.mapper;

import java.time.LocalDate;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TransactionMapper {

  @Mapping(target = "user", source = "user")
  @Mapping(target = "date", source = "date")
  @Mapping(target = "channel", source = "message.channel")
  @Mapping(target = "chatId", source = "message.chatId")
  @Mapping(target = "messageId", source = "message.messageId")
  Transaction toEntity(TransactionData data, ChatMessage message, User user, LocalDate date);

  TransactionData toData(Transaction transaction);
}
