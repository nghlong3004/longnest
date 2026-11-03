package me.nghlong3004.longnest.repository;

import java.util.UUID;
import me.nghlong3004.longnest.model.entity.TransactionActionInput;
import me.nghlong3004.longnest.model.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionActionInputRepository
    extends JpaRepository<TransactionActionInput, UUID> {
  boolean existsByChannelAndExternalUserIdAndChatIdAndMessageId(
      Channel channel, String externalUserId, String chatId, String messageId);
}
