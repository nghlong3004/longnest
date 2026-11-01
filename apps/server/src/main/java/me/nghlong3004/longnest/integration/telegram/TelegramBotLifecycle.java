package me.nghlong3004.longnest.integration.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramBotLifecycle implements SmartLifecycle {

  private final TelegramBotsLongPollingApplication polling;
  private final TelegramProperties properties;
  private final TelegramEchoHandler handler;
  private volatile boolean running;

  @Override
  public synchronized void start() {
    if (running) {
      return;
    }
    try {
      polling.registerBot(properties.token(), handler);
      running = true;
      log.info("Telegram echo bot started");
    } catch (TelegramApiException exception) {
      // SDK exceptions may include request URLs containing the bot token.
      throw new IllegalStateException("Unable to register Telegram bot; check token and connectivity");
    }
  }

  @Override
  public synchronized void stop() {
    if (!running) {
      return;
    }
    try {
      polling.unregisterBot(properties.token());
    } catch (TelegramApiException exception) {
      log.warn("Telegram bot stopped with an SDK cleanup error");
    } finally {
      running = false;
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
