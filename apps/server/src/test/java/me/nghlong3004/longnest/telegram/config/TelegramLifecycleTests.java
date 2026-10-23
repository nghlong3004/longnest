package me.nghlong3004.longnest.telegram.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.List;
import java.time.Clock;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.presentation.UiMessages;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.updates.GetWebhookInfo;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.WebhookInfo;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramLifecycleTests {
  private final TelegramSettings settings = new TelegramSettings("123:" + "x".repeat(30), 77);
  private final TelegramClient client = mock(TelegramClient.class);
  private final TelegramCapture capture = mock(TelegramCapture.class);
  private final TelegramBotsLongPollingApplication app = mock(TelegramBotsLongPollingApplication.class);

  @Test
  void productionHttpDeadlinesExceedConfiguredSdkLongPoll() {
    try (var executor = new SimpleAsyncTaskExecutor()) {
      var lifecycle = new TelegramConfiguration().telegramLifecycle(settings,
          new PersonalOwner("personal"), mock(TransactionMapper.class), mock(LedgerClient.class),
          Clock.systemUTC(), mock(UiMessages.class), new ControlProofs(),
          new TransactionInterpreter(), executor);
      try {
        var http = (OkHttpClient) ReflectionTestUtils.getField(lifecycle, "http");
        assertThat(http.connectTimeoutMillis()).isEqualTo(5000);
        assertThat(http.readTimeoutMillis()).isEqualTo(35000);
        assertThat(http.callTimeoutMillis()).isEqualTo(40000);
      } finally {
        lifecycle.stop();
      }
    }
  }

  @Test
  void enabledWithoutPrivateTokenAndUserFailsBeforeSessionCreation() {
    new ApplicationContextRunner().withUserConfiguration(TelegramConfiguration.class)
        .withPropertyValues("spring.profiles.active=ledger", "longnest.telegram.enabled=true")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure()).hasStackTraceContaining(
              "Telegram requires a valid token and one numeric user ID");
        });
  }

  @Test
  void wrongBotIdentityRefusesWebhookCheckAndRegistration() throws Exception {
    var checked = new CountDownLatch(1);
    when(client.execute(any(GetMe.class))).thenAnswer(call -> {
      checked.countDown();
      return new User(999L, "wrong", true);
    });
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    var http = new OkHttpClient();
    try (var executor = new SimpleAsyncTaskExecutor()) {
      executor.setVirtualThreads(true);
      var lifecycle = new TelegramLifecycle(settings, client, capture, app, scheduler, http,
          executor);
      lifecycle.start();
      assertThat(checked.await(2, TimeUnit.SECONDS)).isTrue();
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
      while (lifecycle.isRunning() && System.nanoTime() < deadline) Thread.onSpinWait();
      assertThat(lifecycle.isRunning()).isFalse();
      verify(client, never()).execute(any(GetWebhookInfo.class));
      verify(app, never()).registerBot(anyString(), any(), any(), any());
    } finally {
      scheduler.shutdownNow();
    }
  }

  @Test
  void existingWebhookPreventsSdkRegistrationAndDeletion() throws Exception {
    when(client.execute(any(GetMe.class))).thenReturn(new User(123L, "bot", true));
    var checked = new CountDownLatch(1);
    var info = new WebhookInfo();
    info.setUrl("https://existing.example/webhook");
    when(client.execute(any(GetWebhookInfo.class))).thenAnswer(call -> {
      checked.countDown();
      return info;
    });
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    var http = new OkHttpClient();
    try (var executor = new SimpleAsyncTaskExecutor()) {
      executor.setVirtualThreads(true);
      var lifecycle = new TelegramLifecycle(settings, client, capture, app, scheduler, http,
          executor);
      lifecycle.start();
      assertThat(checked.await(2, TimeUnit.SECONDS)).isTrue();
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
      while (lifecycle.isRunning() && System.nanoTime() < deadline) Thread.onSpinWait();
      assertThat(lifecycle.isRunning()).isFalse();
      verify(app, never()).registerBot(anyString(), any(), any(), any());
      verify(capture, never()).recoverPending(any());
    } finally {
      scheduler.shutdownNow();
    }
  }

  @Test
  void stopOwnsSessionSchedulerAndHttpWithoutPostStopRegistration() throws Exception {
    when(client.execute(any(GetMe.class))).thenReturn(new User(123L, "bot", true));
    var registered = new CountDownLatch(1);
    var info = new WebhookInfo();
    info.setUrl("");
    when(client.execute(any(GetWebhookInfo.class))).thenReturn(info);
    var session = mock(BotSession.class);
    var generator = new AtomicReference<Function<Integer, GetUpdates>>();
    when(app.registerBot(anyString(), any(), any(), any())).thenAnswer(call -> {
      generator.set(call.getArgument(2));
      registered.countDown();
      return session;
    });
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    var http = new OkHttpClient();
    try (var executor = new SimpleAsyncTaskExecutor()) {
      executor.setVirtualThreads(true);
      var lifecycle = new TelegramLifecycle(settings, client, capture, app, scheduler, http,
          executor);
      lifecycle.start();
      assertThat(registered.await(2, TimeUnit.SECONDS)).isTrue();
      var request = generator.get().apply(9);
      assertThat(request.getOffset()).isEqualTo(10);
      assertThat(request.getLimit()).isEqualTo(100);
      assertThat(request.getTimeout()).isEqualTo(30);
      assertThat(request.getAllowedUpdates()).containsExactly("message", "callback_query");
      lifecycle.stop();
      assertThat(lifecycle.isRunning()).isFalse();
      assertThat(scheduler.isShutdown()).isTrue();
      assertThat(http.dispatcher().executorService().isShutdown()).isTrue();
      verify(app).close();
      verify(session).stop();
      verify(capture).recoverPending(any());
    }
  }

  @Test
  void stopWaitsForStartedManagedWorkAndSkipsRemainingBatch() throws Exception {
    when(client.execute(any(GetMe.class))).thenReturn(new User(123L, "bot", true));
    var info = new WebhookInfo();
    info.setUrl("");
    when(client.execute(any(GetWebhookInfo.class))).thenReturn(info);
    var registered = new CountDownLatch(1);
    when(app.registerBot(anyString(), any(), any(), any())).thenAnswer(call -> {
      registered.countDown();
      return mock(BotSession.class);
    });
    var entered = new CountDownLatch(1);
    var release = new Semaphore(0);
    var virtual = new AtomicBoolean();
    when(capture.handle(any())).thenAnswer(call -> {
      virtual.set(Thread.currentThread().isVirtual());
      entered.countDown();
      release.acquireUninterruptibly();
      return true;
    });
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    var http = new OkHttpClient();
    try (var executor = new SimpleAsyncTaskExecutor()) {
      executor.setVirtualThreads(true);
      var lifecycle = new TelegramLifecycle(settings, client, capture, app, scheduler, http,
          executor);
      lifecycle.start();
      assertThat(registered.await(2, TimeUnit.SECONDS)).isTrue();
      var first = new Update();
      first.setUpdateId(1);
      var second = new Update();
      second.setUpdateId(2);
      var consumer = Thread.ofPlatform().start(() -> {
        try {
          lifecycle.consume(List.of(first, second));
        } catch (RuntimeException stopped) {
          // SDK scheduled task also terminates on this sanitized failure.
        }
      });
      assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
      var shutdown = Thread.ofPlatform().start(lifecycle::stop);
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
      while (lifecycle.isRunning() && System.nanoTime() < deadline) Thread.onSpinWait();
      assertThat(lifecycle.isRunning()).isFalse();
      release.release();
      shutdown.join(3000);
      consumer.join(3000);
      assertThat(shutdown.isAlive()).isFalse();
      assertThat(consumer.isAlive()).isFalse();
      assertThat(virtual).isTrue();
      verify(capture, times(1)).handle(any());
    } finally {
      release.release();
      scheduler.shutdownNow();
    }
  }

  @Test
  void stopWaitsForStartupRecoveryAndNeverRegistersAfterIt() throws Exception {
    when(client.execute(any(GetMe.class))).thenReturn(new User(123L, "bot", true));
    var info = new WebhookInfo();
    info.setUrl("");
    when(client.execute(any(GetWebhookInfo.class))).thenReturn(info);
    var entered = new CountDownLatch(1);
    var release = new Semaphore(0);
    doAnswer(call -> {
      entered.countDown();
      release.acquireUninterruptibly();
      return null;
    }).when(capture).recoverPending(any());
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    var http = new OkHttpClient();
    try (var executor = new SimpleAsyncTaskExecutor()) {
      executor.setVirtualThreads(true);
      var lifecycle = new TelegramLifecycle(settings, client, capture, app, scheduler, http,
          executor);
      lifecycle.start();
      assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
      var shutdown = Thread.ofPlatform().start(lifecycle::stop);
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
      while (lifecycle.isRunning() && System.nanoTime() < deadline) Thread.onSpinWait();
      assertThat(lifecycle.isRunning()).isFalse();
      release.release();
      shutdown.join(3000);
      assertThat(shutdown.isAlive()).isFalse();
      verify(app, never()).registerBot(anyString(), any(), any(), any());
    } finally {
      release.release();
      scheduler.shutdownNow();
    }
  }
}
