package me.nghlong3004.longnest.repository;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  Optional<Transaction> findFirstByUser_IdAndChannelAndChatIdAndMessageId(
      UUID userId, Channel channel, String chatId, String messageId);
}
