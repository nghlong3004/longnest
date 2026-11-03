package me.nghlong3004.longnest.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionTotals;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Transaction> findByIdAndUser_IdAndChannelAndChatId(
      UUID id, UUID userId, Channel channel, String chatId);

  Optional<Transaction> findFirstByUser_IdAndChannelAndChatIdAndMessageId(
      UUID userId, Channel channel, String chatId, String messageId);

  List<Transaction> findByUser_IdAndTypeAndDateBetween(
      UUID userId, TransactionType type, LocalDate from, LocalDate to, Pageable page);

  @Query(
      """
      select t from Transaction t
      where t.user.id = :userId and t.type = :type and t.date between :from and :to
        and (t.date < :cursorDate or (t.date = :cursorDate and t.id < :cursorId))
      order by t.date desc, t.id desc
      """)
  List<Transaction> findAfterCursor(
      UUID userId,
      TransactionType type,
      LocalDate from,
      LocalDate to,
      LocalDate cursorDate,
      UUID cursorId,
      Pageable page);

  @Query(
      """
      select new me.nghlong3004.longnest.model.dto.TransactionTotals(coalesce(sum(t.amount), 0), count(t))
      from Transaction t
      where t.user.id = :userId and t.type = :type and t.date between :from and :to
      """)
  TransactionTotals totals(UUID userId, TransactionType type, LocalDate from, LocalDate to);
}
