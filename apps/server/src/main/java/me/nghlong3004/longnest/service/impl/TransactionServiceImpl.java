package me.nghlong3004.longnest.service.impl;

import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.mapper.DraftMapper;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionRecordingResult;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.entity.TransactionDraft;
import me.nghlong3004.longnest.model.entity.User;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.repository.DeletedTransactionSourceRepository;
import me.nghlong3004.longnest.repository.DraftMessageLinkRepository;
import me.nghlong3004.longnest.repository.TransactionActionInputRepository;
import me.nghlong3004.longnest.repository.TransactionDraftRepository;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.service.TransactionService;
import me.nghlong3004.longnest.service.UserIdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

  private final UserIdentityService identities;
  private final TransactionRepository transactions;
  private final TransactionMapper mapper;
  private final ZoneId ledgerTimeZone;
  private final TransactionDraftRepository drafts;
  private final DraftMessageLinkRepository links;
  private final DraftMapper draftMapper;
  private final DeletedTransactionSourceRepository deletedSources;
  private final TransactionActionInputRepository actionInputs;

  @Override
  @Transactional(readOnly = true)
  public boolean isMutationInput(ChatMessage message) {
    return actionInputs.existsByChannelAndExternalUserIdAndChatIdAndMessageId(
        message.channel(), message.externalUserId(), message.chatId(), message.messageId());
  }

  private void rejectMutationInput(ChatMessage message) {
    if (isMutationInput(message)) {
      throw new ResourceException(ErrorCode.TRANSACTION_INPUT_PROCESSED);
    }
  }

  @Override
  @Transactional
  public Optional<TransactionData> findRecorded(ChatMessage message) {
    rejectMutationInput(message);
    return identities
        .find(message)
        .flatMap(identity -> findBySource(identity, message))
        .map(mapper::toData);
  }

  @Override
  @Transactional
  public TransactionRecordingResult recordOrContinue(TransactionData data, ChatMessage message) {
    rejectMutationInput(message);
    var identity = identities.resolve(message);
    var existing = findBySource(identity, message);
    if (existing.isPresent()) {
      return new TransactionRecordingResult(mapper.toData(existing.get()), null);
    }
    var target = findDraftForSource(identity, message);
    if (target.isPresent()) {
      var draft = target.get();
      var receipt =
          links.findByDraft_User_IdAndChannelAndChatIdAndMessageId(
              identity.getUser().getId(), message.channel(), message.chatId(), message.messageId());
      if (receipt.isEmpty()) {
        links.saveAndFlush(draftMapper.toLink(draft, message, message.messageId()));
      }
      return new TransactionRecordingResult(null, draftMapper.toContext(draft, true));
    }
    return new TransactionRecordingResult(
        mapper.toData(insert(data, message, identity.getUser())), null);
  }

  @Override
  @Transactional
  public Transaction record(TransactionData data, ChatMessage message) {
    return recordSource(data, message, null);
  }

  @Override
  @Transactional
  public Transaction recordFromDraft(TransactionData data, ChatMessage message, UUID draftId) {
    return recordSource(data, message, Objects.requireNonNull(draftId, "Draft ID is required"));
  }

  private Transaction recordSource(TransactionData data, ChatMessage message, UUID draftId) {
    rejectMutationInput(message);
    var identity = identities.resolve(message);
    var user = identity.getUser();
    if (draftId != null) {
      var draft =
          drafts
              .findByIdAndUser_IdAndChannelAndChatId(
                  draftId, user.getId(), message.channel(), message.chatId())
              .orElseThrow(() -> new ResourceException(ErrorCode.DRAFT_NOT_FOUND));
      if (draft.getStatus() != DraftStatus.PENDING
          || !draft.getMessageId().equals(message.messageId())) {
        throw new ResourceException(ErrorCode.DRAFT_CONTEXT_CHANGED);
      }
    }
    var existing = findBySource(identity, message);
    if (existing.isPresent()) {
      return existing.get();
    }
    if (draftId == null && findDraftForSource(identity, message).isPresent()) {
      throw new ResourceException(ErrorCode.DRAFT_CONTEXT_CHANGED);
    }
    return insert(data, message, user);
  }

  private Transaction insert(TransactionData data, ChatMessage message, User user) {
    var date =
        data.date() == null ? message.sentAt().atZone(ledgerTimeZone).toLocalDate() : data.date();
    return transactions.saveAndFlush(mapper.toEntity(data, message, user, date));
  }

  private Optional<TransactionDraft> findDraftForSource(
      UserIdentity identity, ChatMessage message) {
    var userId = identity.getUser().getId();
    return links
        .findByDraft_User_IdAndChannelAndChatIdAndMessageId(
            userId, message.channel(), message.chatId(), message.messageId())
        .map(link -> link.getDraft())
        .or(() ->
            drafts.findFirstByUser_IdAndChannelAndChatIdAndStatusOrderByUpdatedAtDesc(
                userId, message.channel(), message.chatId(), DraftStatus.PENDING));
  }

  private Optional<Transaction> findBySource(UserIdentity identity, ChatMessage message) {
    rejectMutationInput(message);
    if (deletedSources.existsByUserIdAndChannelAndChatIdAndMessageId(
        identity.getUser().getId(), message.channel(), message.chatId(), message.messageId())) {
      throw new ResourceException(ErrorCode.TRANSACTION_DELETED);
    }
    return transactions.findFirstByUser_IdAndChannelAndChatIdAndMessageId(
        identity.getUser().getId(), message.channel(), message.chatId(), message.messageId());
  }
}
