package me.nghlong3004.longnest.integration.telegram;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.DraftCancellation;
import me.nghlong3004.longnest.model.dto.TransactionPageRequest;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.service.ChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramMessageHandler implements LongPollingUpdateConsumer {

  private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
  private static final Duration TYPING_REFRESH_INTERVAL = Duration.ofSeconds(4);
  private static final int MAX_MESSAGE_LENGTH = 4096;
  private static final String CANCEL_PREFIX = "cancel_draft:";

  private final TelegramClient client;
  private final TelegramProperties properties;
  private final ChatService chatService;
  private final GlobalExceptionHandler exceptionHandler;
  private final TaskScheduler telegramTypingScheduler;
  private final TelegramTransactionQueryCodec queryCodec;
  private final TelegramTransactionActions transactionActions;

  @Override
  public void consume(List<Update> updates) {
    for (var update : updates) {
      if (update.hasCallbackQuery()) {
        handleCallback(update.getCallbackQuery());
      } else if (update.hasMessage()) {
        var message = update.getMessage();
        if (message.hasText() && isOwner(message, message.getFrom())) {
          handleText(message, update.getUpdateId());
        }
      }
    }
  }

  private boolean isOwner(Message message, User sender) {
    return message.getChat() != null
        && message.isUserMessage()
        && sender != null
        && sender.getId() != null
        && !Boolean.TRUE.equals(sender.getIsBot())
        && properties.users().contains(sender.getId())
        && sender.getId().equals(message.getChatId());
  }

  private void handleText(Message message, Integer updateId) {
    if (transactionActions.handleText(message)) {
      return;
    }
    var chatId = message.getChatId().toString();
    var typing =
        telegramTypingScheduler.scheduleWithFixedDelay(
            () -> sendTyping(chatId), TYPING_REFRESH_INTERVAL);
    ChatMessage source = null;
    ChatReply reply;
    try {
      source = toChatMessage(message, message.getFrom(), message.getText());
      reply = chatService.reply(source);
    } catch (RuntimeException exception) {
      reply = ChatReply.text(exceptionHandler.handle(exception, VIETNAMESE));
    } finally {
      typing.cancel(false);
    }
    try {
      var first = sendResponse(chatId, message.getMessageId(), reply);
      if (source != null) {
        chatService.rememberReply(source, reply);
      }
      if (reply.draftId() != null) {
        try {
          chatService.registerQuestion(source, reply, first.getMessageId().toString());
        } catch (RuntimeException exception) {
          // The question is already delivered; plain replies can still use the persisted draft.
          exceptionHandler.handle(exception, VIETNAMESE);
        }
      }
      log.info("Telegram reply sent for update {}", updateId);
    } catch (TelegramApiException exception) {
      log.warn("Telegram reply could not be sent for update {}", updateId);
    }
  }

  private void handleCallback(CallbackQuery callback) {
    if (!(callback.getMessage() instanceof Message message)
        || !isOwner(message, callback.getFrom())
        || callback.getId() == null
        || callback.getData() == null) {
      return;
    }
    if (callback.getData().startsWith(TelegramTransactionQueryCodec.PREFIX)) {
      if (transactionActions.isRevokedPage(message.getChatId().toString(), callback.getData())) {
        answerCallback(callback.getId(), "Trang cũ đã thay đổi. Hãy dùng danh sách cập nhật.");
        return;
      }
      handleQueryCallback(callback, message);
      return;
    }
    if (callback.getData().startsWith(TelegramTransactionActions.PREFIX)) {
      transactionActions.handleCallback(callback, message);
      return;
    }
    if (!callback.getData().startsWith(CANCEL_PREFIX)) {
      return;
    }
    UUID draftId;
    try {
      draftId = UUID.fromString(callback.getData().substring(CANCEL_PREFIX.length()));
    } catch (IllegalArgumentException exception) {
      answerCallback(callback.getId(), "Nút này không hợp lệ.");
      return;
    }
    try {
      var source = toChatMessage(message, callback.getFrom(), "/cancel");
      var result = chatService.cancelDraft(source, draftId);
      if (result.status() == DraftStatus.CANCELLED) {
        finishCancellation(callback.getId(), message, result);
      } else {
        answerCallback(callback.getId(), result.reply());
      }
    } catch (RuntimeException exception) {
      answerCallback(callback.getId(), exceptionHandler.handle(exception, VIETNAMESE));
    }
  }

  private void handleQueryCallback(CallbackQuery callback, Message message) {
    TransactionPageRequest request;
    try {
      request = queryCodec.decode(callback.getData());
    } catch (IllegalArgumentException exception) {
      answerCallback(callback.getId(), "Nút này không hợp lệ.");
      return;
    }
    // Acknowledge before accessing the database to stop Telegram's button spinner.
    answerCallback(callback.getId(), null);
    var source =
        new ChatMessage(
            Channel.TELEGRAM,
            callback.getFrom().getId().toString(),
            message.getChatId().toString(),
            Integer.toString(request.messageId()),
            Instant.now(),
            "Xem thêm");
    ChatReply reply;
    try {
      reply = chatService.nextPage(source, request);
    } catch (RuntimeException exception) {
      reply = ChatReply.text(exceptionHandler.handle(exception, VIETNAMESE));
    }
    try {
      sendResponse(message.getChatId().toString(), request.messageId(), reply);
    } catch (TelegramApiException exception) {
      log.warn("Transaction page could not be sent for callback {}", callback.getId());
    }
  }

  private void finishCancellation(String callbackId, Message question, DraftCancellation result) {
    answerCallback(callbackId, result.changed() ? null : "Khoản này đã hủy rồi.");
    var chatId = question.getChatId().toString();
    try {
      client.execute(new DeleteMessage(chatId, question.getMessageId()));
    } catch (TelegramApiException exception) {
      log.warn("Cancelled draft question could not be deleted for callback {}", callbackId);
    }
    if (result.changed()) {
      try {
        sendReply(
            chatId, Integer.valueOf(result.originalMessageId()), ChatReply.text(result.reply()));
      } catch (TelegramApiException exception) {
        log.warn("Cancellation confirmation could not be sent for callback {}", callbackId);
      }
    }
  }

  private void answerCallback(String callbackId, String text) {
    try {
      client.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).text(text).build());
    } catch (TelegramApiException exception) {
      log.warn("Telegram callback could not be answered for callback {}", callbackId);
    }
  }

  private ChatMessage toChatMessage(Message message, User sender, String text) {
    var repliedTo = message.getReplyToMessage();
    return new ChatMessage(
        Channel.TELEGRAM,
        sender.getId().toString(),
        message.getChatId().toString(),
        message.getMessageId().toString(),
        Instant.ofEpochSecond(message.getDate()),
        text,
        repliedTo == null ? null : repliedTo.getMessageId().toString(),
        ((sender.getFirstName() == null ? "" : sender.getFirstName())
                + " "
                + (sender.getLastName() == null ? "" : sender.getLastName()))
            .strip());
  }

  private void sendTyping(String chatId) {
    try {
      client.execute(new SendChatAction(chatId, ActionType.TYPING.toString()));
    } catch (TelegramApiException exception) {
      log.warn("Telegram typing status could not be sent for chat {}", chatId);
    }
  }

  private Message sendResponse(String chatId, Integer replyToMessageId, ChatReply reply)
      throws TelegramApiException {
    return reply.transactions() == null
        ? sendReply(chatId, replyToMessageId, reply)
        : transactionActions.sendPage(chatId, replyToMessageId, reply);
  }

  private Message sendReply(String chatId, Integer replyToMessageId, ChatReply reply)
      throws TelegramApiException {
    return sendReply(chatId, replyToMessageId, reply, null);
  }

  private Message sendReply(
      String chatId, Integer replyToMessageId, ChatReply reply, InlineKeyboardMarkup keyboard)
      throws TelegramApiException {
    var text = reply.text();
    Message first = null;
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
      if (end < text.length()
          && Character.isHighSurrogate(text.charAt(end - 1))
          && Character.isLowSurrogate(text.charAt(end))) {
        end--;
      }
      var request = new SendMessage(chatId, text.substring(start, end));
      request.setReplyParameters(
          ReplyParameters.builder()
              .messageId(replyToMessageId)
              .allowSendingWithoutReply(true)
              .build());
      if (start == 0 && reply.draftId() != null) {
        var cancel =
            InlineKeyboardButton.builder()
                .text("Hủy")
                .callbackData(CANCEL_PREFIX + reply.draftId())
                .build();
        request.setReplyMarkup(new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(cancel))));
      }
      if (start == 0 && keyboard != null) {
        request.setReplyMarkup(keyboard);
      }
      var sent = client.execute(request);
      if (start == 0) {
        first = sent;
      }
      start = end;
    }
    return first;
  }
}
