package me.nghlong3004.longnest.integration.telegram;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.service.ChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramMessageHandler implements LongPollingUpdateConsumer {
  private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
  private static final Duration TYPING_REFRESH_INTERVAL = Duration.ofSeconds(4);

  private final TelegramMessageSender sender;
  private final TelegramProperties properties;
  private final ChatService chatService;
  private final GlobalExceptionHandler exceptionHandler;
  private final TaskScheduler telegramTypingScheduler;
  private final TelegramTransactionActions transactionActions;
  private final TelegramCallbackRouter callbackRouter;
  private final TelegramMessageMapper mapper;

  @Override
  public void consume(List<Update> updates) {
    for (var update : updates) {
      try {
        if (update.hasCallbackQuery()) {
          handleCallback(update.getCallbackQuery());
        } else if (update.hasMessage()) {
          var message = update.getMessage();
          if (message.hasText() && isOwner(message, message.getFrom())) {
            handleText(message, update.getUpdateId());
          }
        }
      } catch (RuntimeException exception) {
        exceptionHandler.handle(exception, VIETNAMESE);
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
    var typing = scheduleTyping(chatId);
    ChatMessage source = null;
    ChatReply reply;
    try {
      source = mapper.toChatMessage(message, message.getFrom(), message.getText());
      reply = chatService.reply(source);
    } catch (RuntimeException exception) {
      reply = ChatReply.text(exceptionHandler.handle(exception, VIETNAMESE));
    } finally {
      if (typing != null) {
        typing.cancel(false);
      }
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
          // The question is delivered; plain replies can still use the persisted draft.
          exceptionHandler.handle(exception, VIETNAMESE);
        }
      }
      log.info("Telegram reply sent for update {}", updateId);
    } catch (TelegramApiException exception) {
      log.warn("Telegram reply could not be sent for update {}", updateId);
    }
  }

  private ScheduledFuture<?> scheduleTyping(String chat) {
    try {
      return telegramTypingScheduler.scheduleWithFixedDelay(
          () -> sender.typing(chat), TYPING_REFRESH_INTERVAL);
    } catch (RuntimeException exception) {
      log.warn("Telegram typing refresh could not be scheduled");
      return null;
    }
  }

  private void handleCallback(CallbackQuery callback) {
    if (!(callback.getMessage() instanceof Message message)
        || !isOwner(message, callback.getFrom())
        || callback.getId() == null
        || callback.getData() == null) {
      return;
    }
    callbackRouter.route(callback, message);
  }

  private Message sendResponse(String chat, int original, ChatReply reply)
      throws TelegramApiException {
    if (reply.transactions() != null) {
      return transactionActions.sendPage(chat, original, reply);
    }
    var sent =
        sender.sendText(
            chat,
            original,
            reply.text(),
            cancelKeyboard(reply.draftId()),
            TelegramMessageSender.KeyboardPlacement.FIRST);
    return sent.isEmpty() ? null : sent.getFirst();
  }

  private InlineKeyboardMarkup cancelKeyboard(UUID draftId) {
    if (draftId == null) {
      return null;
    }
    var cancel =
        InlineKeyboardButton.builder()
            .text("Hủy")
            .callbackData(TelegramDraftCallbackHandler.PREFIX + draftId)
            .build();
    return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(cancel)));
  }
}
