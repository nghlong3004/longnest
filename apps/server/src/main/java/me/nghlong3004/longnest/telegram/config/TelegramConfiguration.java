package me.nghlong3004.longnest.telegram.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import me.nghlong3004.longnest.input.InputInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.presentation.UiMessages;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration(proxyBeanMethods = false)
@Profile("ledger & !ledger-init")
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramConfiguration {
  @Bean
  TelegramSettings telegramSettings(@Value("${longnest.telegram.token:}") String token,
      @Value("${longnest.telegram.user:0}") long user) {
    return new TelegramSettings(token, user);
  }

  @Bean
  TelegramLifecycle telegramLifecycle(TelegramSettings settings, PersonalOwner owner,
      TransactionMapper mapper, LedgerClient ledger, Clock clock, UiMessages messages,
      ControlProofs proofs, InputInterpreter interpreter,
      @Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor) {
    // SDK uses Jackson 2 internally; Boot's Jackson 3 bean is untouched.
    var http = new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(35))
        .followRedirects(false).followSslRedirects(false)
        .callTimeout(Duration.ofSeconds(40))
        .addInterceptor(chain -> {
          try {
            return chain.proceed(chain.request());
          } catch (IOException unavailable) {
            // OkHttp exceptions may contain the token-bearing request URL.
            throw new IOException("Telegram transport unavailable");
          } catch (RuntimeException unavailable) {
            throw new IOException("Telegram transport unavailable");
          }
        }).build();
    TelegramClient client = new OkHttpTelegramClient(http, settings.getToken());
    var capture = new TelegramCapture(client, settings, owner, interpreter,
        mapper, ledger, new TelegramReplies(messages), clock,
        new TelegramNavigation(clock, new SecureRandom()), proofs);
    var scheduler = Executors.newSingleThreadScheduledExecutor(
        Thread.ofPlatform().name("telegram-sdk-poll").factory());
    var application = new TelegramBotsLongPollingApplication(
        ObjectMapper::new, () -> http, () -> scheduler);
    return new TelegramLifecycle(settings, client, capture, application, scheduler, http, executor);
  }
}
