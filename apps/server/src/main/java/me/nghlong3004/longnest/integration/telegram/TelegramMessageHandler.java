package me.nghlong3004.longnest.integration.telegram;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.service.ChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
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

  private final TelegramClient client;
  private final TelegramProperties properties;
  private final ChatService chatService;
  private final GlobalExceptionHandler exceptionHandler;

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
      String reply;
      try {
        reply = chatService.reply(message.getText());
      } catch (RuntimeException exception) {
        reply = exceptionHandler.handle(exception, VIETNAMESE);
      }
      try {
        sendReply(message.getChatId().toString(), reply);
        log.info("Telegram reply sent for update {}", update.getUpdateId());
      } catch (TelegramApiException exception) {
        log.warn("Telegram reply could not be sent for update {}", update.getUpdateId());
      }
    }
  }

  private void sendReply(String chatId, String text) throws TelegramApiException {
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + 4096, text.length());
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
