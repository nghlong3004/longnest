package me.nghlong3004.longnest.service.impl;

import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.ChatResponse;
import me.nghlong3004.longnest.model.dto.DraftCancellation;
import me.nghlong3004.longnest.model.dto.DraftContext;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.enums.ChatAction;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  private final ObjectProvider<ChatClient> clients;
  private final JsonMapper jsonMapper;
  private final Validator validator;
  private final TransactionService transactions;
  private final ZoneId ledgerTimeZone;
  private final DraftService drafts;

  @Override
  public ChatReply reply(ChatMessage message) {
    validateMessage(message);
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
        return ChatReply.text(confirmation(recorded.get()));
      }
      var context = drafts.findContext(message).orElse(null);
      if (context != null && (context.replayed() || context.status() != DraftStatus.PENDING)) {
        return fromDraft(context);
      }
      if (context == null && message.replyToMessageId() != null) {
        return ChatReply.text(
            "Không tìm thấy bản nháp cho câu hỏi này. Anh gửi lại khoản đầy đủ nhé.");
      }
      var response = interpret(client, message, context);
      if (response.action() == ChatAction.CHAT) {
        return ChatReply.text(response.reply());
      }
      if (context != null || response.action() == ChatAction.ASK_CLARIFICATION) {
        return fromDraft(drafts.apply(response, message, context));
      }
      var result = transactions.recordOrContinue(response.transaction(), message);
      return result.draft() == null
          ? ChatReply.text(confirmation(result.recorded()))
          : fromDraft(result.draft());
    } catch (ResourceException exception) {
      if (exception.getErrorCode() != ErrorCode.DRAFT_CONTEXT_CHANGED) {
        throw exception;
      }
      return currentReply(message);
    } catch (DataAccessException | TransactionException exception) {
      throw new ResourceException(ErrorCode.TRANSACTION_SAVE_FAILED, exception);
    }
  }

  private ChatReply currentReply(ChatMessage message) {
    var recorded = transactions.findRecorded(message);
    if (recorded.isPresent()) {
      return ChatReply.text(confirmation(recorded.get()));
    }
    return drafts
        .findContext(message)
        .map(this::fromDraft)
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
        return new DraftCancellation(null, "Không có bản nháp đang chờ.", false, null);
      }
      var status = context.get().status();
      var reply =
          status == DraftStatus.CANCELLED
              ? cancellationConfirmation(context.get().transaction())
              : "Khoản này đã ghi rồi, không thể hủy bằng nút này.";
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

  private ChatReply fromDraft(DraftContext context) {
    return switch (context.status()) {
      case PENDING -> new ChatReply(context.questionText(), context.id());
      case CONFIRMED -> ChatReply.text(confirmation(context.transaction()));
      case CANCELLED ->
          ChatReply.text("Khoản này đã hủy. Anh gửi tin mới nếu muốn ghi khoản khác nhé.");
    };
  }

  private boolean isCancelCommand(String text) {
    return switch (text.strip().toLowerCase(Locale.ROOT)) {
      case "/cancel", "hủy", "huỷ" -> true;
      default -> false;
    };
  }

  private ChatResponse interpret(ChatClient client, ChatMessage message, DraftContext context) {
    String answer;
    try {
      var sentAt = context == null ? message.sentAt() : context.originalSentAt();
      var referenceDate = sentAt.atZone(ledgerTimeZone).toLocalDate();
      var messages = new ArrayList<Message>();
      var currentDate = message.sentAt().atZone(ledgerTimeZone).toLocalDate();
      messages.add(
          new SystemMessage(
              "Ngày gửi tin nhắn gốc: "
                  + referenceDate
                  + ". Ngày gửi tin nhắn mới: "
                  + currentDate
                  + ". Múi giờ: "
                  + ledgerTimeZone
                  + ". Ngày tương đối trong tin nhắn mới tham chiếu ngày gửi tin nhắn mới; "
                  + "giữ ngày đã biết của draft nếu người dùng không sửa ngày."));
      if (context != null) {
        messages.add(new UserMessage(context.originalText()));
        messages.add(
            new AssistantMessage(
                "Thông tin draft đang có: "
                    + jsonMapper.writeValueAsString(context.transaction())
                    + "\nCâu hỏi hiện tại: "
                    + context.questionText()));
      }
      messages.add(new UserMessage(message.text()));
      answer = client.prompt().messages(messages).call().content();
    } catch (RestClientException | TransientAiException | NonTransientAiException exception) {
      throw new ResourceException(ErrorCode.CHATBOT_UNAVAILABLE, exception);
    }
    if (answer == null || answer.isBlank()) {
      throw new ResourceException(ErrorCode.CHATBOT_EMPTY_RESPONSE);
    }
    return parse(answer, context);
  }

  private ChatResponse parse(String answer, DraftContext context) {
    ChatResponse response;
    try {
      response =
          jsonMapper
              .readerFor(ChatResponse.class)
              .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .with(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
              .with(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
              .readValue(answer);
    } catch (JacksonException exception) {
      throw new ResourceException(ErrorCode.CHATBOT_INVALID_RESPONSE, exception);
    }
    if (response == null
        || !validator.validate(response).isEmpty()
        || !isTransactionConsistent(response, context)) {
      throw new ResourceException(ErrorCode.CHATBOT_INVALID_RESPONSE);
    }
    return response;
  }

  private boolean isTransactionConsistent(ChatResponse response, DraftContext context) {
    var data = response.transaction();
    if (data != null && data.type() == TransactionType.INCOME && data.category() != null) {
      return false;
    }
    return switch (response.action()) {
      case READY ->
          data != null && (context != null || (data.type() != null && data.amount() != null));
      case CHAT -> data == null;
      case ASK_CLARIFICATION -> true;
    };
  }

  private String confirmation(TransactionData saved) {
    var type = saved.type() == TransactionType.INCOME ? "thu" : "chi";
    var description = saved.description();
    var detail = description == null || description.isBlank() ? "" : " — " + description;
    return "Đã ghi khoản " + type + " " + formatAmount(saved.amount()) + "đ" + detail + ".";
  }

  private String cancellationConfirmation(TransactionData data) {
    var description = data.description();
    String label;
    if (description != null && !description.isBlank()) {
      var name = description.strip();
      if (name.codePointCount(0, name.length()) > 120) {
        name = name.substring(0, name.offsetByCodePoints(0, 120)) + "…";
      }
      label = "“" + name + "”";
    } else if (data.type() != null) {
      label = data.type() == TransactionType.INCOME ? "thu" : "chi";
    } else {
      label = "đang bổ sung";
    }
    var amount = data.amount() == null ? "" : " (" + formatAmount(data.amount()) + "đ)";
    return "Em hủy khoản " + label + amount + " rồi nhé.";
  }

  private String formatAmount(BigDecimal amount) {
    var numbers = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    numbers.setMaximumFractionDigits(2);
    return numbers.format(amount);
  }
}
