package me.nghlong3004.longnest.telegram.config;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.model.TelegramFailure;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import okhttp3.OkHttpClient;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.AsyncTaskExecutor;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.api.methods.updates.GetWebhookInfo;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** SDK poller is a synchronous barrier: no next getUpdates until this batch is handled. */
@Slf4j
@RequiredArgsConstructor
public class TelegramLifecycle implements SmartLifecycle {
  private final TelegramSettings settings;
  private final TelegramClient client;
  private final TelegramCapture capture;
  private final TelegramBotsLongPollingApplication application;
  private final ScheduledExecutorService scheduler;
  private final OkHttpClient http;
  private final AsyncTaskExecutor executor;
  private final Object processing = new Object();
  private volatile boolean running;
  private volatile boolean closed;
  private volatile BotSession session;
  private volatile Future<?> startup;
  private volatile Future<?> work;

  @Override
  public synchronized void start() {
    if (running) return;
    if (closed) throw new IllegalStateException("Telegram lifecycle is closed");
    running = true;
    try {
      startup = executor.submit(this::register);
    } catch (RuntimeException rejected) {
      running = false;
      closed = true;
      closeResources();
      throw new IllegalStateException("Telegram worker could not start");
    }
  }

  private void register() {
    try {
      if (!running) return;
      var identity = client.execute(new GetMe());
      if (identity == null || !Boolean.TRUE.equals(identity.getIsBot())
          || identity.getId() == null
          || !settings.getNamespace().equals("tg:" + identity.getId() + ":")) {
        throw new TelegramFailure(true, 3);
      }
      if (!running) return;
      var webhook = client.execute(new GetWebhookInfo());
      if (webhook == null || webhook.getUrl() == null || !webhook.getUrl().isBlank()) {
        throw new TelegramFailure(true, 3);
      }
      if (!running) return;
      synchronized (processing) {
        if (!running) return;
        capture.recoverPending(() -> running && !Thread.currentThread().isInterrupted());
      }
      if (!running) return;
      synchronized (this) {
        if (running) session = application.registerBot(settings.getToken(),
            () -> TelegramUrl.DEFAULT_URL, offset -> {
              // Keep SDK offset handling; bound the long poll below HTTP deadlines.
              var request = new DefaultGetUpdatesGenerator(List.of("message", "callback_query"))
                  .apply(offset);
              request.setTimeout(30);
              return request;
            }, this::consume);
      }
    } catch (Exception unavailable) {
      fail("Telegram registration/recovery unavailable; operator restart required");
    }
  }

  void consume(List<Update> batch) {
    for (Update update : batch) {
      while (running) {
        try {
          // Each service proxy runs on Boot's managed executor with explicit HostContext.
          work = executor.submit(() -> {
            synchronized (processing) {
              if (!running) return false;
              return capture.handle(update);
            }
          });
          if (Boolean.TRUE.equals(work.get())) break;
          if (!running) throw new InterruptedException();
          log.warn("Telegram/ledger outcome unavailable; retrying the same event");
          TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          fail("Telegram poll interrupted; restart required");
          throw new TelegramFailure(false, 3);
        } catch (ExecutionException unavailable) {
          if (unavailable.getCause() instanceof TelegramFailure failure && failure.isFatal()) {
            fail("Telegram access/configuration unavailable; operator action required");
            throw new TelegramFailure(true, 3);
          }
          log.warn("Telegram/ledger handling unavailable; retrying the same event");
          try {
            TimeUnit.SECONDS.sleep(3);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            fail("Telegram poll interrupted; restart required");
            throw new TelegramFailure(false, 3);
          }
        } catch (RuntimeException unavailable) {
          fail("Telegram worker unavailable; restart required");
          throw new TelegramFailure(false, 3);
        }
      }
      if (!running) throw new TelegramFailure(false, 3);
    }
  }

  private void fail(String safeMessage) {
    log.error(safeMessage);
    stop();
  }

  @Override
  public void stop() {
    if (closed) return;
    running = false;
    closed = true;
    Future<?> pending = startup;
    if (pending != null) pending.cancel(true);
    pending = work;
    if (pending != null) pending.cancel(true);
    synchronized (this) {
      BotSession current = session;
      if (current != null) current.stop();
    }
    http.dispatcher().cancelAll();
    scheduler.shutdownNow();
    synchronized (processing) {
      // All already-started application work finished; no new work passes the running check.
    }
    closeResources();
  }

  private void closeResources() {
    try {
      application.close();
    } catch (Exception ignored) {
      log.warn("Telegram SDK close incomplete");
    }
    http.dispatcher().executorService().shutdownNow();
    http.connectionPool().evictAll();
    if (http.cache() != null) {
      try {
        http.cache().close();
      } catch (IOException ignored) {
        log.warn("Telegram HTTP cache close incomplete");
      }
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
