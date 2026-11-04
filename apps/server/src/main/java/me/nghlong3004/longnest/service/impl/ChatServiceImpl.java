package me.nghlong3004.longnest.service.impl;

import jakarta.validation.Validator;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.conversation.ConversationHistory;
import me.nghlong3004.longnest.conversation.TransactionModelInterpreter;
import me.nghlong3004.longnest.conversation.VietnameseQueryRecovery;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.DraftCancellation;
import me.nghlong3004.longnest.model.dto.DraftContext;
import me.nghlong3004.longnest.model.dto.TransactionCursor;
import me.nghlong3004.longnest.model.dto.TransactionPageRequest;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.enums.ChatAction;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.presentation.TransactionMessageFormatter;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import me.nghlong3004.longnest.service.TransactionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  private static final String DELETED_REPLY =
      "Em đã xóa vĩnh viễn khoản này khỏi sổ. Tin cũ không tạo lại khoản này nhé.";

  private final ObjectProvider<ChatClient> clients;
  private final Validator validator;
  private final TransactionService transactions;
  private final DraftService drafts;
  private final TransactionQueryService queries;
  private final TransactionMessageFormatter formatter;
  private final ConversationHistory history;
  private final TransactionModelInterpreter interpreter;

  @Override
  public ChatReply reply(ChatMessage message) {
    validateMessage(message);
    synchronized (history.lock(message)) {
      var reply = replyInOrder(message);
      rememberReply(message, reply);
      return reply;
    }
  }

  @Override
  public void rememberReply(ChatMessage message, ChatReply reply) {
    validateMessage(message);
    synchronized (history.lock(message)) {
      var intent = VietnameseQueryRecovery.interpret(message.text(), history.queryType(message));
      var queryType =
          reply.transactions() == null ? intent.type() : reply.transactions().filter().type();
      var text = reply.text();
      if (reply.transactions() != null) {
        for (var item : reply.transactions().items()) text += "\n" + formatter.entry(item);
      }
      history.remember(
          message, text, intent.viewing() || reply.transactions() != null ? queryType : null);
    }
  }

  private ChatReply replyInOrder(ChatMessage message) {
    if (transactions.isMutationInput(message)) {
      return ChatReply.text(
          "Em đã xử lý tin này trong thao tác sửa/xóa rồi. Xem lại sổ hoặc gửi tin mới nhé.");
    }
    if (isCancelCommand(message.text())) {
      return ChatReply.text(cancelDraft(message, null).reply());
    }
    var client = clients.getIfAvailable();
    if (client == null) {
      return ChatReply.text(message.text());
    }
    try {
      var recorded = transactions.findRecorded(message);
      if (recorded.isPresent()) {
        return ChatReply.text(formatter.confirmation(recorded.get(), message.displayName()));
      }
      var context = drafts.findContext(message).orElse(null);
      if (context != null && context.replayed()) {
        return fromDraft(context, message.displayName());
      }
      if (isPureGreeting(message.text())) {
        return ChatReply.text(formatter.greeting(message.displayName()));
      }
      var response = interpreter.interpret(client, message, context);
      var intent = VietnameseQueryRecovery.interpret(message.text(), history.queryType(message));
      if (intent.query() != null) {
        return view(message, intent.query(), null);
      }
      if (response.action() == ChatAction.QUERY) {
        return view(message, response.query(), null);
      }
      if (intent.viewing()) {
        return ChatReply.text(
            intent.type() == null
                ? "Em tra khoản thu hay khoản chi nhé?"
                : "Cho em xin khoảng thời gian muốn xem khoản "
                    + (intent.type() == TransactionType.EXPENSE ? "chi" : "thu")
                    + " nhé.");
      }
      if (response.action() == ChatAction.CHAT) {
        return ChatReply.text(response.reply());
      }
      if (VietnameseQueryRecovery.acknowledgment(message.text())) {
        return ChatReply.text("Không có gì, cứ nhắn em khi cần nhé.");
      }
      if (context != null && context.status() != DraftStatus.PENDING) {
        return fromDraft(context, message.displayName());
      }
      if (context == null && message.replyToMessageId() != null) {
        return ChatReply.text(
            "Em không tìm thấy bản nháp cho câu hỏi này. Gửi lại khoản đầy đủ giúp em nhé.");
      }
      if (response.action() == ChatAction.READY
          && (((context == null || context.status() != DraftStatus.PENDING)
                  && (!VietnameseQueryRecovery.canCreate(message.text())
                      || (VietnameseQueryRecovery.hasDateCue(message.text())
                          && response.transaction().date() == null)))
              || (context != null
                  && context.status() == DraftStatus.PENDING
                  && context.transaction().amount() == null
                  && !VietnameseQueryRecovery.canAnswerAmount(message.text())))) {
        return ChatReply.text(
            context != null && context.status() == DraftStatus.PENDING
                ? "Bổ sung số tiền để em ghi khoản này nhé."
                : "Em chưa ghi thêm khoản nào. Nói rõ khoản mới muốn ghi giúp em nhé.");
      }
      if (context != null || response.action() == ChatAction.ASK_CLARIFICATION) {
        return fromDraft(drafts.apply(response, message, context), message.displayName());
      }
      var result = transactions.recordOrContinue(response.transaction(), message);
      return result.draft() == null
          ? ChatReply.text(formatter.confirmation(result.recorded(), message.displayName()))
          : fromDraft(result.draft(), message.displayName());
    } catch (ResourceException exception) {
      if (exception.getErrorCode() == ErrorCode.TRANSACTION_INPUT_PROCESSED) {
        return ChatReply.text(
            "Em đã xử lý tin này trong thao tác sửa/xóa rồi. Xem lại sổ hoặc gửi tin mới nhé.");
      }
      if (exception.getErrorCode() == ErrorCode.TRANSACTION_DELETED) {
        return ChatReply.text(DELETED_REPLY);
      }
      if (exception.getErrorCode() != ErrorCode.DRAFT_CONTEXT_CHANGED) {
        throw exception;
      }
      return currentReply(message);
    } catch (DataAccessException | TransactionException exception) {
      throw new ResourceException(ErrorCode.TRANSACTION_SAVE_FAILED, exception);
    }
  }

  @Override
  public ChatReply nextPage(ChatMessage message, TransactionPageRequest request) {
    validateMessage(message);
    if (request == null
        || request.query() == null
        || request.cursor() == null
        || request.messageId() <= 0) {
      throw new ResourceException(ErrorCode.QUERY_INVALID_PARAMETERS);
    }
    return view(message, request.query(), request.cursor());
  }

  private ChatReply view(ChatMessage message, TransactionQuery query, TransactionCursor cursor) {
    try {
      var page = queries.view(message, query, cursor);
      return new ChatReply(formatter.summary(page), null, page);
    } catch (DataAccessException | TransactionException exception) {
      throw new ResourceException(ErrorCode.QUERY_FAILED, exception);
    }
  }

  private ChatReply currentReply(ChatMessage message) {
    var recorded = transactions.findRecorded(message);
    if (recorded.isPresent()) {
      return ChatReply.text(formatter.confirmation(recorded.get(), message.displayName()));
    }
    return drafts
        .findContext(message)
        .map(context -> fromDraft(context, message.displayName()))
        .orElseThrow(() -> new ResourceException(ErrorCode.DRAFT_CONTEXT_CHANGED));
  }

  @Override
  public void registerQuestion(ChatMessage message, ChatReply reply, String questionMessageId) {
    drafts.registerQuestion(message, reply, questionMessageId);
  }

  @Override
  public DraftCancellation cancelDraft(ChatMessage message, UUID draftId) {
    validateMessage(message);
    try {
      var context = drafts.cancel(message, draftId);
      if (context.isEmpty()) {
        return new DraftCancellation(null, "Em không có bản nháp nào đang chờ nhé.", false, null);
      }
      var status = context.get().status();
      var reply =
          status == DraftStatus.DELETED
              ? DELETED_REPLY
              : status == DraftStatus.CANCELLED
                  ? formatter.cancellationConfirmation(context.get().transaction())
                  : "Em đã ghi khoản này rồi. Nếu muốn xóa, mở khoản đó trong danh sách giúp em"
                      + " nhé.";
      return new DraftCancellation(
          status,
          reply,
          status == DraftStatus.CANCELLED && !context.get().replayed(),
          context.get().originalMessageId());
    } catch (DataAccessException | TransactionException exception) {
      throw new ResourceException(ErrorCode.DRAFT_UPDATE_FAILED, exception);
    }
  }

  private void validateMessage(ChatMessage message) {
    if (message == null || !validator.validate(message).isEmpty()) {
      throw new IllegalArgumentException("Valid incoming message metadata is required");
    }
  }

  private ChatReply fromDraft(DraftContext context, String displayName) {
    return switch (context.status()) {
      case PENDING -> new ChatReply(context.questionText(), context.id());
      case CONFIRMED -> ChatReply.text(formatter.confirmation(context.transaction(), displayName));
      case DELETED -> ChatReply.text(DELETED_REPLY);
      case CANCELLED ->
          ChatReply.text("Em đã hủy khoản này rồi. Gửi tin mới nếu muốn ghi khoản khác nhé.");
    };
  }

  private boolean isPureGreeting(String text) {
    return text.strip()
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ")
        .matches("(?:(?:xin )?chào(?: em)?|(?:hello|hi)(?: em)?|/start)[.!?,]*");
  }

  private boolean isCancelCommand(String text) {
    return switch (text.strip().toLowerCase(Locale.ROOT)) {
      case "/cancel", "hủy", "huỷ" -> true;
      default -> false;
    };
  }
}
