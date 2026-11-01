package me.nghlong3004.longnest.service.impl;

import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.entity.User;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.repository.UserRepository;
import me.nghlong3004.longnest.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

  private final UserRepository users;
  private final UserIdentityRepository identities;
  private final TransactionRepository transactions;
  private final TransactionMapper mapper;
  private final ZoneId ledgerTimeZone;

  @Override
  @Transactional
  public Transaction record(TransactionData data, ChatMessage message) {
    // Lock before checking the source so concurrent deliveries cannot both insert.
    var identity =
        identities.findByChannelAndExternalUserId(message.channel(), message.externalUserId())
            .orElseGet(() -> createIdentity(message));
    var user = identity.getUser();
    var existing =
        transactions.findFirstByUser_IdAndChannelAndChatIdAndMessageId(
            user.getId(), message.channel(), message.chatId(), message.messageId());
    if (existing.isPresent()) {
      return existing.get();
    }
    var date =
        data.date() == null
            ? message.sentAt().atZone(ledgerTimeZone).toLocalDate()
            : data.date();
    return transactions.saveAndFlush(mapper.toEntity(data, message, user, date));
  }

  private UserIdentity createIdentity(ChatMessage message) {
    return identities.saveAndFlush(
        UserIdentity.builder()
            .user(users.save(User.builder().build()))
            .channel(message.channel())
            .externalUserId(message.externalUserId())
            .build());
  }
}
