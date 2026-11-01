package me.nghlong3004.longnest.service.impl;

import jakarta.validation.Validator;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatResponse;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.enums.ChatAction;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.TransactionService;
import org.springframework.ai.chat.client.ChatClient;
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

  @Override
  public String reply(ChatMessage message) {
    if (message == null || !validator.validate(message).isEmpty()) {
      throw new IllegalArgumentException("Valid incoming message metadata is required");
    }
    var client = clients.getIfAvailable();
    if (client == null) {
      return message.text();
    }
    var response = interpret(client, message);
    if (response.action() != ChatAction.READY) {
      return response.reply();
    }
    Transaction saved;
    try {
      saved = transactions.record(response.transaction(), message);
    } catch (DataAccessException | TransactionException exception) {
      throw new ResourceException(ErrorCode.TRANSACTION_SAVE_FAILED, exception);
    }
    return confirmation(saved);
  }

  private ChatResponse interpret(ChatClient client, ChatMessage message) {
    String answer;
    try {
      var referenceDate = message.sentAt().atZone(ledgerTimeZone).toLocalDate();
      answer =
          client.prompt()
              .messages(
                  new SystemMessage(
                      "Ngày gửi tin nhắn gốc: " + referenceDate
                          + ". Múi giờ: " + ledgerTimeZone
                          + ". Dùng mốc này để hiểu ngày tương đối."),
                  new UserMessage(message.text()))
              .call()
              .content();
    } catch (RestClientException | TransientAiException | NonTransientAiException exception) {
      throw new ResourceException(ErrorCode.CHATBOT_UNAVAILABLE, exception);
    }
    if (answer == null || answer.isBlank()) {
      throw new ResourceException(ErrorCode.CHATBOT_EMPTY_RESPONSE);
    }
    return parse(answer);
  }

  private ChatResponse parse(String answer) {
    ChatResponse response;
    try {
      response =
          jsonMapper.readerFor(ChatResponse.class)
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
        || !isTransactionConsistent(response)) {
      throw new ResourceException(ErrorCode.CHATBOT_INVALID_RESPONSE);
    }
    return response;
  }

  private boolean isTransactionConsistent(ChatResponse response) {
    var data = response.transaction();
    if (data != null && data.type() == TransactionType.INCOME && data.category() != null) {
      return false;
    }
    return switch (response.action()) {
      case READY -> data != null && data.type() != null && data.amount() != null;
      case CHAT -> data == null;
      case ASK_CLARIFICATION -> true;
    };
  }

  private String confirmation(Transaction saved) {
    var numbers = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    numbers.setMaximumFractionDigits(2);
    var type = saved.getType() == TransactionType.INCOME ? "thu" : "chi";
    var description = saved.getDescription();
    var detail = description == null || description.isBlank() ? "" : " — " + description;
    return "Đã ghi khoản " + type + " " + numbers.format(saved.getAmount()) + "đ" + detail + ".";
  }
}
