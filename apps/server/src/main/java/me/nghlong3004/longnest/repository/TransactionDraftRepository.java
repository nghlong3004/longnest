package me.nghlong3004.longnest.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.longnest.model.entity.TransactionDraft;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TransactionDraftRepository extends JpaRepository<TransactionDraft, UUID> {

  Optional<TransactionDraft> findByTransaction_Id(UUID transactionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TransactionDraft> findByIdAndUser_IdAndChannelAndChatId(
      UUID id, UUID userId, Channel channel, String chatId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TransactionDraft> findFirstByUser_IdAndChannelAndChatIdAndStatusOrderByUpdatedAtDesc(
      UUID userId, Channel channel, String chatId, DraftStatus status);
}
