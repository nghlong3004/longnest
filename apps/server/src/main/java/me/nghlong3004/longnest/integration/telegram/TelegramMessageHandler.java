package me.nghlong3004.longnest.integration.telegram;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.service.ChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
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

  private final TelegramClient client;
  private final TelegramProperties properties;
  private final ChatService chatService;
  private final GlobalExceptionHandler exceptionHandler;
  private final TaskScheduler telegramTypingScheduler;

  @Override
  public void consume(List<Update> updates) {
    for (var update : updates) {
      if (!update.hasMessage()) {
        continue;
      }
      var message = update.getMessage();
      if (!message.hasText()
          || message.getChat() == null
          || !message.isUserMessage()
          || message.getFrom() == null
          || Boolean.TRUE.equals(message.getFrom().getIsBot())
          || !properties.user().equals(message.getFrom().getId())
          || !properties.user().equals(message.getChatId())) {
        continue;
      }
      var chatId = message.getChatId().toString();
      var typing =
          telegramTypingScheduler.scheduleWithFixedDelay(
              () -> sendTyping(chatId), TYPING_REFRESH_INTERVAL);
      String reply;
      try {
        reply =
            chatService.reply(
                new ChatMessage(
                    Channel.TELEGRAM,
                    message.getFrom().getId().toString(),
                    chatId,
                    message.getMessageId().toString(),
                    Instant.ofEpochSecond(message.getDate()),
                    message.getText()));
      } catch (RuntimeException exception) {
        reply = exceptionHandler.handle(exception, VIETNAMESE);
      } finally {
        typing.cancel(false);
      }
      try {
        sendReply(chatId, reply);
        log.info("Telegram reply sent for update {}", update.getUpdateId());
      } catch (TelegramApiException exception) {
        log.warn("Telegram reply could not be sent for update {}", update.getUpdateId());
      }
    }
  }

  private void sendTyping(String chatId) {
    try {
      client.execute(new SendChatAction(chatId, ActionType.TYPING.toString()));
    } catch (TelegramApiException exception) {
      log.warn("Telegram typing status could not be sent for chat {}", chatId);
    }
  }

  private void sendReply(String chatId, String text) throws TelegramApiException {
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
      if (end < text.length()
          && Character.isHighSurrogate(text.charAt(end - 1))
          && Character.isLowSurrogate(text.charAt(end))) {
        end--;
      }
      client.execute(new SendMessage(chatId, text.substring(start, end)));
      start = end;
    }
  }
}
