package me.nghlong3004.longnest.integration.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfiguration {

  @Bean
  ObjectMapper telegramObjectMapper() {
    // Jackson 2 is confined to the Telegram SDK boundary; Spring Boot uses Jackson 3.
    return new ObjectMapper();
  }

  @Bean
  OkHttpClient telegramHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(70))
        .writeTimeout(Duration.ofSeconds(10))
        .callTimeout(Duration.ofSeconds(75))
        .retryOnConnectionFailure(false)
        .build();
  }

  @Bean(destroyMethod = "shutdownNow")
  ScheduledExecutorService telegramPollingExecutor() {
    return Executors.newSingleThreadScheduledExecutor(
        Thread.ofVirtual().name("telegram-polling-", 0).factory());
  }

  @Bean
  ThreadPoolTaskScheduler telegramTypingScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadFactory(Thread.ofVirtual().name("telegram-typing-", 0).factory());
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }

  @Bean
  TelegramClient telegramClient(OkHttpClient telegramHttpClient, TelegramProperties properties) {
    return new OkHttpTelegramClient(telegramHttpClient, properties.token());
  }

  @Bean(destroyMethod = "close")
  TelegramBotsLongPollingApplication telegramPollingApplication(
      ObjectMapper telegramObjectMapper,
      OkHttpClient telegramHttpClient,
      ScheduledExecutorService telegramPollingExecutor) {
    return new TelegramBotsLongPollingApplication(
        () -> telegramObjectMapper, () -> telegramHttpClient, () -> telegramPollingExecutor);
  }

  @Bean
  DisposableBean telegramHttpCleanup(OkHttpClient telegramHttpClient) {
    return () -> {
      telegramHttpClient.dispatcher().cancelAll();
      telegramHttpClient.connectionPool().evictAll();
      telegramHttpClient.dispatcher().executorService().shutdown();
    };
  }
}
