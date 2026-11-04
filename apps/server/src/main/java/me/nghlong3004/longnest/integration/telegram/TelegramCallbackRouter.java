package me.nghlong3004.longnest.integration.telegram;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramCallbackRouter {
  private final List<TelegramCallbackHandler> handlers;

  public void route(CallbackQuery callback, Message message) {
    for (var handler : handlers) {
      if (handler.supports(callback.getData())) {
        handler.handleCallback(callback, message);
        return;
      }
    }
  }
}
