package me.nghlong3004.longnest.integration.telegram;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class TelegramEchoHandler implements LongPollingUpdateConsumer {

  private final TelegramClient client;
  private final TelegramProperties properties;

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
      try {
        client.execute(new SendMessage(message.getChatId().toString(), message.getText()));
        log.info("Telegram echo sent for update {}", update.getUpdateId());
      } catch (TelegramApiException exception) {
        log.warn("Telegram echo could not be sent for update {}", update.getUpdateId());
      }
    }
  }
}
