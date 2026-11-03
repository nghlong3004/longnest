package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

class TelegramBotLifecycleTests {

  private final TelegramBotsLongPollingApplication polling =
      mock(TelegramBotsLongPollingApplication.class);
  private final TelegramProperties properties =
      new TelegramProperties("synthetic-token", Set.of(123L, 789L));
  private final TelegramMessageHandler handler = mock(TelegramMessageHandler.class);
  private final TelegramBotLifecycle lifecycle =
      new TelegramBotLifecycle(polling, properties, handler);

  @Test
  void registersOnceAndUnregistersOnceAcrossRepeatedLifecycleCalls() throws TelegramApiException {
    assertThat(lifecycle.isRunning()).isFalse();
    lifecycle.start();
    lifecycle.start();
    assertThat(lifecycle.isRunning()).isTrue();
    verify(polling).registerBot(properties.token(), handler);

    lifecycle.stop();
    lifecycle.stop();
    assertThat(lifecycle.isRunning()).isFalse();
    verify(polling).unregisterBot(properties.token());
  }

  @Test
  void failsStartupWithoutExposingSdkExceptionOrToken() throws TelegramApiException {
    doThrow(new TelegramApiException("sensitive SDK detail: " + properties.token()))
        .when(polling)
        .registerBot(properties.token(), handler);

    assertThatThrownBy(lifecycle::start)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining(properties.token())
        .hasNoCause();
    assertThat(lifecycle.isRunning()).isFalse();
  }

  @Test
  void canStartAgainAfterStopping() throws TelegramApiException {
    lifecycle.start();
    lifecycle.stop();
    lifecycle.start();
    verify(polling, times(2)).registerBot(properties.token(), handler);
    assertThat(lifecycle.isRunning()).isTrue();
  }
}
