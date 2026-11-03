package me.nghlong3004.longnest.repository;

import java.util.UUID;
import me.nghlong3004.longnest.model.entity.DeletedTransactionSource;
import me.nghlong3004.longnest.model.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedTransactionSourceRepository
    extends JpaRepository<DeletedTransactionSource, UUID> {
  boolean existsByUserIdAndChannelAndChatIdAndMessageId(
      UUID userId, Channel channel, String chatId, String messageId);
}
