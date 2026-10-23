package me.nghlong3004.longnest.telegram.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator;
import org.telegram.telegrambots.longpolling.util.ExponentialBackOff;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/** In-process interceptor exercises SDK's real callback/offset code, without a socket. */
class TelegramSdkSessionTests {
  @Test
  void sdkClientSendsSdkMessageThroughJackson2Boundary() throws Exception {
    var calls = new AtomicInteger();
    var http = new OkHttpClient.Builder().addInterceptor(chain -> {
      assertThat(chain.request().url().encodedPath()).endsWith("/sendmessage");
      var body = new Buffer();
      chain.request().body().writeTo(body);
      assertThat(body.readUtf8()).contains("\"chat_id\":\"77\"", "\"text\":\"Saved\"");
      calls.incrementAndGet();
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
          .code(200).message("OK").body(ResponseBody.create(
              "{\"ok\":true,\"result\":{\"message_id\":1,\"date\":1,"
                  + "\"chat\":{\"id\":77,\"type\":\"private\"}}}",
              okhttp3.MediaType.get("application/json"))).build();
    }).build();
    try {
      var client = new OkHttpTelegramClient(http, "123:" + "x".repeat(30));
      var sent = client.execute(SendMessage.builder().chatId("77").text("Saved").build());
      assertThat(sent.getMessageId()).isEqualTo(1);
      assertThat(sent.getChat().getId()).isEqualTo(77);
      assertThat(calls).hasValue(1);
    } finally {
      http.dispatcher().executorService().shutdownNow();
      http.connectionPool().evictAll();
    }
  }

  @Test
  void sdkMarksWholeBatchBeforeCallbackButFailureStopsNextConfirmingPoll() throws Exception {
    var polls = new AtomicInteger();
    var offsets = new java.util.concurrent.CopyOnWriteArrayList<Integer>();
    var entered = new CountDownLatch(1);
    var restarted = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var holder = new AtomicReference<BotSession>();
    var http = new OkHttpClient.Builder().addInterceptor(chain -> {
      String path = chain.request().url().encodedPath();
      String body;
      if (path.endsWith("/deleteWebhook")) {
        body = "{\"ok\":true,\"result\":true}";
      } else {
        polls.incrementAndGet();
        body = "{\"ok\":true,\"result\":[{\"update_id\":9},{\"update_id\":10}]}";
      }
      return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
          .code(200).message("OK").body(ResponseBody.create(body,
              okhttp3.MediaType.get("application/json"))).build();
    }).build();
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      var session = new BotSession(new ObjectMapper(), http, scheduler,
          "123:" + "x".repeat(30), () -> TelegramUrl.DEFAULT_URL,
          offset -> {
            offsets.add(offset);
            return new DefaultGetUpdatesGenerator(List.of("message")).apply(offset);
          }, ExponentialBackOff::new, updates -> {
            assertThat(updates).hasSize(2);
            entered.countDown();
            try {
              release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
              Thread.currentThread().interrupt();
            }
            holder.get().stop();
            throw new IllegalStateException("synthetic undurable handoff");
          });
      holder.set(session);
      session.start();
      assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(session.getLastReceivedUpdate().get()).isEqualTo(10);
      assertThat(polls).hasValue(1);
      release.countDown();
      Thread.sleep(100);
      assertThat(polls).hasValue(1);
      assertThat(offsets).containsExactly(0);
      session.close();
      var reopened = new BotSession(new ObjectMapper(), http, scheduler,
          "123:" + "x".repeat(30), () -> TelegramUrl.DEFAULT_URL,
          offset -> {
            offsets.add(offset);
            return new DefaultGetUpdatesGenerator(List.of("message")).apply(offset);
          }, ExponentialBackOff::new, updates -> {
            restarted.countDown();
            holder.get().stop();
            throw new IllegalStateException("synthetic restart stop");
          });
      holder.set(reopened);
      reopened.start();
      assertThat(restarted.await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(offsets).containsExactly(0, 0);
      assertThat(polls).hasValue(2);
      reopened.close();
    } finally {
      release.countDown();
      scheduler.shutdownNow();
      http.dispatcher().executorService().shutdownNow();
      http.connectionPool().evictAll();
    }
  }
}
