package me.nghlong3004.longnest.service.impl;

import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.mapper.DraftMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.ChatResponse;
import me.nghlong3004.longnest.model.dto.DraftContext;
import me.nghlong3004.longnest.model.entity.TransactionDraft;
import me.nghlong3004.longnest.model.enums.ChatAction;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.repository.DraftMessageLinkRepository;
import me.nghlong3004.longnest.repository.TransactionDraftRepository;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionService;
import me.nghlong3004.longnest.service.UserIdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DraftServiceImpl implements DraftService {

  private final UserIdentityService identities;
  private final TransactionDraftRepository drafts;
  private final DraftMessageLinkRepository links;
  private final TransactionService transactions;
  private final DraftMapper mapper;
  private final ZoneId ledgerTimeZone;

  @Override
  public Optional<DraftContext> findContext(ChatMessage message) {
    return identities
        .find(message)
        .flatMap(identity -> {
          var userId = identity.getUser().getId();
          var processed = linked(userId, message, message.messageId());
          if (processed.isPresent()) {
            return processed.map(draft -> snapshot(draft, true));
          }
          return target(userId, message).map(draft -> snapshot(draft, false));
        });
  }

  @Override
  public DraftContext apply(ChatResponse response, ChatMessage message, DraftContext expected) {
    if (response.action() == ChatAction.CHAT) {
      throw new IllegalArgumentException("Chat output cannot update a financial draft");
    }
    var user = identities.resolve(message).getUser();
    var processed = linked(user.getId(), message, message.messageId());
    if (processed.isPresent()) {
      return snapshot(processed.get(), true);
    }
    TransactionDraft draft;
    if (expected == null) {
      if (transactions.findRecorded(message).isPresent()) {
        throw new ResourceException(ErrorCode.DRAFT_CONTEXT_CHANGED);
      }
      var pending = pending(user.getId(), message);
      if (pending.isPresent()) {
        link(pending.get(), message, message.messageId());
        return snapshot(pending.get(), true);
      }
      if (response.action() != ChatAction.ASK_CLARIFICATION) {
        throw new IllegalArgumentException("A new draft requires a clarification question");
      }
      var data = response.transaction();
      var date =
          data == null || data.date() == null
              ? message.sentAt().atZone(ledgerTimeZone).toLocalDate()
              : data.date();
      draft = drafts.saveAndFlush(mapper.toEntity(data, message, user, date));
    } else {
      draft = owned(expected.id(), user.getId(), message);
      if (draft.getStatus() != DraftStatus.PENDING || draft.getVersion() != expected.version()) {
        link(draft, message, message.messageId());
        return snapshot(draft, true);
      }
      var data = response.transaction();
      if (data != null) {
        var type = data.type() == null ? draft.getType() : data.type();
        if (type == TransactionType.INCOME && data.category() != null) {
          throw new ResourceException(ErrorCode.CHATBOT_INVALID_RESPONSE);
        }
        draft.revise(
            data.type(),
            data.amount(),
            data.category(),
            data.description(),
            data.location(),
            data.date(),
            data.timeOfDay());
      }
    }

    if (response.action() == ChatAction.READY) {
      if (draft.getType() == null || draft.getAmount() == null) {
        throw new ResourceException(ErrorCode.CHATBOT_INVALID_RESPONSE);
      }
      var saved =
          transactions.recordFromDraft(
              mapper.toData(draft), mapper.toOriginalMessage(draft, message), draft.getId());
      draft.confirm(saved);
    } else {
      draft.askForClarification(response.reply());
    }
    drafts.saveAndFlush(draft);
    link(draft, message, message.messageId());
    return snapshot(draft, false);
  }

  @Override
  public void registerQuestion(ChatMessage message, ChatReply reply, String questionMessageId) {
    var identity =
        identities
            .find(message)
            .orElseThrow(() -> new ResourceException(ErrorCode.DRAFT_NOT_FOUND));
    var draft = owned(reply.draftId(), identity.getUser().getId(), message);
    link(draft, message, questionMessageId);
    if (draft.getStatus() == DraftStatus.PENDING
        && Objects.equals(draft.getQuestionText(), reply.text())) {
      draft.askForClarification(reply.text(), questionMessageId);
    }
    drafts.flush();
  }

  @Override
  public Optional<DraftContext> cancel(ChatMessage message, UUID draftId) {
    var identity = identities.find(message);
    if (identity.isEmpty()) {
      if (draftId != null) {
        throw new ResourceException(ErrorCode.DRAFT_NOT_FOUND);
      }
      return Optional.empty();
    }
    var userId = identity.get().getUser().getId();
    var draft =
        draftId == null
            ? linked(userId, message, message.messageId()).or(() -> target(userId, message))
            : Optional.of(owned(draftId, userId, message));
    return draft.map(current -> {
      boolean changed = current.getStatus() == DraftStatus.PENDING;
      if (changed) {
        current.cancel();
      }
      drafts.flush();
      link(current, message, message.messageId());
      return snapshot(current, !changed);
    });
  }

  private Optional<TransactionDraft> target(UUID userId, ChatMessage message) {
    return message.replyToMessageId() == null
        ? pending(userId, message)
        : linked(userId, message, message.replyToMessageId());
  }

  private Optional<TransactionDraft> pending(UUID userId, ChatMessage message) {
    return drafts.findFirstByUser_IdAndChannelAndChatIdAndStatusOrderByUpdatedAtDesc(
        userId, message.channel(), message.chatId(), DraftStatus.PENDING);
  }

  private TransactionDraft owned(UUID draftId, UUID userId, ChatMessage message) {
    return drafts
        .findByIdAndUser_IdAndChannelAndChatId(draftId, userId, message.channel(), message.chatId())
        .orElseThrow(() -> new ResourceException(ErrorCode.DRAFT_NOT_FOUND));
  }

  private Optional<TransactionDraft> linked(UUID userId, ChatMessage message, String messageId) {
    return links
        .findByDraft_User_IdAndChannelAndChatIdAndMessageId(
            userId, message.channel(), message.chatId(), messageId)
        .map(link -> link.getDraft());
  }

  private void link(TransactionDraft draft, ChatMessage message, String messageId) {
    var existing = linked(draft.getUser().getId(), message, messageId);
    if (existing.isPresent()) {
      if (!existing.get().getId().equals(draft.getId())) {
        throw new IllegalStateException("A source message already belongs to another draft");
      }
      return;
    }
    links.saveAndFlush(mapper.toLink(draft, message, messageId));
  }

  private DraftContext snapshot(TransactionDraft draft, boolean replayed) {
    return mapper.toContext(draft, replayed);
  }
}
