package me.nghlong3004.longnest.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.input.InputInterpreter;
import me.nghlong3004.longnest.conversation.ConversationAssistant;
import me.nghlong3004.longnest.conversation.LedgerTools;
import me.nghlong3004.longnest.ledger.dto.OperationRecovery;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import tools.jackson.databind.json.JsonMapper;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.LedgerException.Code;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.presentation.UiMessagesTests;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramFailure;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** SDK objects at the adapter boundary; no network or database. */
class TelegramAdapterTests {
  private static final Instant SOURCE = Instant.parse("2026-10-19T17:00:00Z");
  private final TelegramClient client = mock(TelegramClient.class);
  private final LedgerClient ledger = mock(LedgerClient.class);
  private final TelegramSettings settings = new TelegramSettings("123:" + "x".repeat(30), 77);

  private TelegramCapture capture() throws TelegramApiException {
    return capture(new TransactionInterpreter());
  }

  private TelegramCapture capture(InputInterpreter interpreter) throws TelegramApiException {
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      SendMessage request = call.getArgument(0);
      assertThat(request.getChatId()).isEqualTo("77");
      var sent = mock(Message.class);
      when(sent.getMessageId()).thenReturn(7);
      when(sent.getChat()).thenReturn(new Chat(77L, "private"));
      return sent;
    });
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var mapper = new TransactionMapper(factory.getValidator(),
          Mappers.getMapper(TransactionRepresentationMapper.class));
      return new TelegramCapture(client, settings, new PersonalOwner("personal"),
          interpreter, mapper, ledger, new TelegramReplies(UiMessagesTests.vietnamese()),
          Clock.fixed(SOURCE.plusSeconds(86400), ZoneOffset.UTC),
          new TelegramNavigation(Clock.fixed(SOURCE, ZoneOffset.UTC), new SecureRandom()),
          new ControlProofs());
    }
  }

  private static Update update(int id, String text) {
    var user = new User(77L, "owner", false);
    var message = new Message();
    message.setMessageId(id);
    message.setFrom(user);
    message.setChat(new Chat(77L, "private"));
    message.setDate(Math.toIntExact(SOURCE.getEpochSecond()));
    message.setText(text);
    var update = new Update();
    update.setUpdateId(id);
    update.setMessage(message);
    return update;
  }

  @Test
  void fallbackReplyFailureRecoversOriginalWithoutSecondModelOrMutation() throws Exception {
    var provider = mock(ChatModel.class);
    when(provider.getOptions()).thenReturn(DeepSeekChatOptions.builder().build());
    when(provider.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
        AssistantMessage.builder().content("").toolCalls(List.of(new AssistantMessage.ToolCall(
            "call-1", "function", "create_transaction", """
            {"type":"EXPENSE","amount":"45 nghìn","currency":"VND",
             "category":"UNCATEGORIZED","note":"bữa trưa","period":"TODAY"}
            """))).build(), ChatGenerationMetadata.builder().finishReason("tool_calls").build()))));
    try (var executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())) {
      var capture = capture();
      try (var validators = Validation.buildDefaultValidatorFactory()) {
        var mapper = new TransactionMapper(validators.getValidator(),
            Mappers.getMapper(TransactionRepresentationMapper.class));
        capture.setConversation(new ConversationAssistant(provider, new PersonalOwner("personal"),
            ledger, Clock.fixed(SOURCE, ZoneOffset.UTC), executor,
            new LedgerTools(ledger, mapper), JsonMapper.builder().build()));
      }
      var host = new HostContext("personal", "tg:123:9", SOURCE, SOURCE);
      var input = new TransactionInput(TransactionType.EXPENSE, "45000", "VND",
          Optional.of("2026-10-20"), Optional.of(Category.UNCATEGORIZED), Optional.of("bữa trưa"));
      when(ledger.recoverOperation(any())).thenReturn(
          new LedgerResult.Ok<>(Optional.empty()),
          new LedgerResult.Ok<>(Optional.of(new OperationRecovery(receipt(host, input, true), null))));
      when(ledger.create(any(), any())).thenReturn(new LedgerResult.Ok<>(receipt(host, input, false)));
      var attempts = new AtomicInteger();
      when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
        if (attempts.getAndIncrement() == 0) throw new TelegramApiException("synthetic delivery failure");
        var sent = new Message();
        sent.setMessageId(7);
        sent.setChat(new Chat(77L, "private"));
        return sent;
      });
      var event = update(9, "Mình vừa trả 45 nghìn cho bữa trưa.");
      assertThatThrownBy(() -> capture.handle(event)).isInstanceOf(TelegramFailure.class);
      assertThat(capture.handle(event)).isTrue();
      verify(provider, times(1)).call(any(Prompt.class));
      verify(ledger, times(1)).create(any(), eq(input));
      verify(ledger, times(2)).recoverOperation(any());
    }
  }

  @Test
  void unavailableRecoveryStopsBeforeProviderAndUnauthorizedUserNeverInterprets() throws Exception {
    var provider = mock(ChatModel.class);
    when(provider.getOptions()).thenReturn(DeepSeekChatOptions.builder().build());
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Failure<>(
        Code.OUTCOME_UNKNOWN, Optional.of("tg:123:9"), true));
    var capture = capture();
    try (var executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory());
        var validators = Validation.buildDefaultValidatorFactory()) {
      var mapper = new TransactionMapper(validators.getValidator(),
          Mappers.getMapper(TransactionRepresentationMapper.class));
      capture.setConversation(new ConversationAssistant(provider, new PersonalOwner("personal"),
          ledger, Clock.fixed(SOURCE, ZoneOffset.UTC), executor,
          new LedgerTools(ledger, mapper), JsonMapper.builder().build()));
    capture.handle(update(9, "Mình vừa trả 45 nghìn cho bữa trưa."));
    verifyNoInteractions(provider);
    verify(ledger, never()).create(any(), any());
    var foreign = update(10, "Mình vừa trả 45 nghìn cho bữa trưa.");
    foreign.getMessage().getFrom().setId(88L);
    capture.handle(foreign);
    verify(ledger, times(1)).recoverOperation(any());
    }
  }

  private static MutationReceipt receipt(HostContext host, TransactionInput input, boolean replay) {
    var view = new TransactionView("42", "1", input.type(), input.amountMinor(), "VND",
        input.category().orElse(Category.UNCATEGORIZED), input.note().orElse(""),
        LocalDate.parse("2026-10-20"), SOURCE.plusSeconds(1), Optional.of(SOURCE),
        TransactionView.State.ACTIVE);
    return new MutationReceipt(host.operationId(), "create", true, replay, view, "1");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ăn trưa 45k", "nhận lương 25tr", "-1,5tr đồ dùng", "+1000000000000 đồ dùng"})
  void clearCaptureUsesTrustedContextAndCommittedReceipt(String text) throws Exception {
    var capture = capture();
    var sent = new AtomicInteger();
    when(ledger.create(any(), any())).thenAnswer(call -> {
      HostContext host = call.getArgument(0);
      TransactionInput input = call.getArgument(1);
      assertThat(host.principal()).isEqualTo("personal");
      assertThat(host.operationId()).isEqualTo("tg:123:9");
      assertThat(host.businessReference()).isEqualTo(SOURCE);
      assertThat(host.messageAt()).isEqualTo(SOURCE);
      assertThat(input.amountMinor()).isIn("45000", "25000000", "1500000", "1000000000000");
      assertThat(sent).hasValue(0);
      return new LedgerResult.Ok<>(receipt(host, input, false));
    });
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      sent.incrementAndGet();
      SendMessage request = call.getArgument(0);
      assertThat(request.getText()).contains("Đã lưu", "#42", "VND", "2026-10-20");
      var reply = mock(Message.class);
      when(reply.getMessageId()).thenReturn(1);
      when(reply.getChat()).thenReturn(new Chat(77L, "private"));
      return reply;
    });
    assertThat(capture.handle(update(9, text))).isTrue();
    assertThat(sent).hasValue(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ăn trưa", "45k", "+45k ăn trưa", "ăn trưa 0", "ăn trưa 45.000",
      "ăn trưa 45k; cà phê 20k", "/edit 42", "/overview", "/start"})
  void ambiguousAndUnsupportedControlsNeverWrite(String text) throws Exception {
    assertThat(capture().handle(update(9, text))).isTrue();
    verifyNoInteractions(ledger);
    verify(client).execute(any(SendMessage.class));
  }

  @Test
  void unauthorizedBotGroupEditedAndUnsupportedUpdatesAreSilent() throws Exception {
    var capture = capture();
    var foreign = update(1, "ăn trưa 45k");
    foreign.getMessage().getFrom().setId(88L);
    assertThat(capture.handle(foreign)).isTrue();
    var bot = update(2, "ăn trưa 45k");
    bot.getMessage().getFrom().setIsBot(true);
    assertThat(capture.handle(bot)).isTrue();
    var group = update(3, "ăn trưa 45k");
    group.getMessage().setChat(new Chat(-77L, "group"));
    assertThat(capture.handle(group)).isTrue();
    var edited = new Update();
    edited.setUpdateId(4);
    edited.setEditedMessage(update(4, "ăn trưa 45k").getMessage());
    assertThat(capture.handle(edited)).isTrue();
    var unsupported = new Update();
    unsupported.setUpdateId(5);
    assertThat(capture.handle(unsupported)).isTrue();
    verifyNoInteractions(ledger);
    verify(client, never()).execute(any(SendMessage.class));
  }

  @Test
  void sameEventReplaysOriginalWhileDistinctEqualMessagesStayIndependent() throws Exception {
    Map<String, MutationReceipt> outcomes = new HashMap<>();
    when(ledger.create(any(), any())).thenAnswer(call -> {
      HostContext host = call.getArgument(0);
      TransactionInput input = call.getArgument(1);
      boolean replay = outcomes.containsKey(host.operationId());
      return new LedgerResult.Ok<>(outcomes.computeIfAbsent(host.operationId(),
          unused -> receipt(host, input, replay)));
    });
    var capture = capture();
    capture.handle(update(9, "ăn trưa 45k"));
    capture.handle(update(9, "ăn trưa 45k"));
    capture.handle(update(10, "ăn trưa 45k"));
    assertThat(outcomes.keySet()).containsExactlyInAnyOrder("tg:123:9", "tg:123:10");
  }

  @Test
  void unknownCommitCannotClaimSavedAndRetryKeepsOperation() throws Exception {
    when(ledger.create(any(), any())).thenReturn(new LedgerResult.Failure<>(Code.OUTCOME_UNKNOWN,
        Optional.of("tg:123:9"), true));
    var capture = capture();
    assertThat(capture.handle(update(9, "ăn trưa 45k"))).isFalse();
    assertThat(capture.handle(update(9, "ăn trưa 45k"))).isFalse();
    verify(client).execute(any(SendMessage.class));
  }

  @Test
  void receiptFailureRetriesSameCommittedOperationAndLabelsReplay() throws Exception {
    var capture = capture();
    var calls = new AtomicInteger();
    when(ledger.create(any(), any())).thenAnswer(call -> {
      HostContext host = call.getArgument(0);
      TransactionInput input = call.getArgument(1);
      return new LedgerResult.Ok<>(receipt(host, input, calls.getAndIncrement() > 0));
    });
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      SendMessage request = call.getArgument(0);
      if (calls.get() == 1) throw new TelegramApiException("synthetic send unavailable");
      assertThat(request.getText()).contains("Kết quả đã lưu ban đầu (phát lại)");
      var sent = mock(Message.class);
      when(sent.getMessageId()).thenReturn(1);
      when(sent.getChat()).thenReturn(new Chat(77L, "private"));
      return sent;
    });
    var event = update(9, "ăn trưa 45k");
    assertThatThrownBy(() -> capture.handle(event)).isInstanceOf(
        TelegramFailure.class);
    assertThat(capture.handle(event)).isTrue();
    assertThat(calls).hasValue(2);
  }

  @Test
  void pendingRecoveryUsesFrozenOperationWithoutReinterpretation() throws Exception {
    var queries = new AtomicInteger();
    when(ledger.pendingCreates(any(), eq("tg:123:")))
        .thenAnswer(call -> new LedgerResult.Ok<>(queries.getAndIncrement() == 0
            ? List.of("tg:123:7") : List.of()));
    when(ledger.recover(any())).thenAnswer(call -> {
      HostContext host = call.getArgument(0);
      assertThat(host.operationId()).isEqualTo("tg:123:7");
      assertThat(host.messageAt()).isNull();
      assertThat(host.businessReference()).isNull();
      return new LedgerResult.Ok<>(receipt(host,
          new TransactionInput(TransactionType.EXPENSE,
              "45000", "VND", Optional.empty(), Optional.of(Category.FOOD), Optional.empty()),
          false));
    });
    capture().recoverPending(() -> true);
    verify(ledger).recover(any());
    assertThat(queries).hasValue(2);
  }

  @Test
  void startupDrainsMoreThanOneBoundedFrozenBatch() throws Exception {
    var remaining = new LinkedHashSet<String>();
    for (int index = 1; index <= 23; index++) remaining.add("tg:123:" + index);
    var queries = new AtomicInteger();
    when(ledger.pendingCreates(any(), eq("tg:123:"))).thenAnswer(call -> {
      queries.incrementAndGet();
      var batch = remaining.stream().limit(10).toList();
      assertThat(batch).hasSizeLessThanOrEqualTo(10);
      return new LedgerResult.Ok<>(batch);
    });
    when(ledger.recover(any())).thenAnswer(call -> {
      HostContext host = call.getArgument(0);
      assertThat(host.businessReference()).isNull();
      assertThat(host.messageAt()).isNull();
      assertThat(remaining.remove(host.operationId())).isTrue();
      return new LedgerResult.Ok<>(receipt(host,
          new TransactionInput(TransactionType.EXPENSE,
              "45000", "VND", Optional.empty(), Optional.of(Category.FOOD), Optional.empty()),
          false));
    });
    capture().recoverPending(() -> true);
    assertThat(remaining).isEmpty();
    assertThat(queries).hasValue(4);
    verify(ledger, times(23)).recover(any());
  }

  @Test
  void startupRecoveryStopsBetweenFrozenOperations() throws Exception {
    var active = new AtomicBoolean(true);
    when(ledger.pendingCreates(any(), eq("tg:123:")))
        .thenReturn(new LedgerResult.Ok<>(List.of("tg:123:1", "tg:123:2")));
    when(ledger.recover(any())).thenAnswer(call -> {
      HostContext host = call.getArgument(0);
      active.set(false);
      return new LedgerResult.Ok<>(receipt(host,
          new TransactionInput(TransactionType.EXPENSE,
              "45000", "VND", Optional.empty(), Optional.of(Category.FOOD), Optional.empty()),
          false));
    });
    assertThatThrownBy(() -> capture().recoverPending(active::get))
        .isInstanceOf(TelegramFailure.class);
    verify(ledger).recover(any());
  }

  @Test
  void failedStartupRecoveryDoesNotPromiseAutomaticRetry() throws Exception {
    when(ledger.pendingCreates(any(), eq("tg:123:")))
        .thenReturn(new LedgerResult.Ok<>(List.of("tg:123:1", "tg:123:2")));
    when(ledger.recover(any())).thenReturn(new LedgerResult.Failure<>(Code.OUTCOME_UNKNOWN,
        Optional.of("tg:123:1"), true));
    var capture = capture();
    assertThatThrownBy(() -> capture.recoverPending(() -> true))
        .isInstanceOf(TelegramFailure.class);
    var message = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(message.capture());
    assertThat(message.getValue().getText()).contains("Khôi phục đã dừng; cần người vận hành xử lý.")
        .doesNotContain("tự thử lại", "Đã lưu");
    verify(ledger).recover(any());
  }

  @Test
  void queriesRouteThroughTrustedContextAndRefreshStaleRecent() throws Exception {
    when(ledger.get(any(), eq("42"))).thenReturn(new LedgerResult.Failure<>(
        Code.NOT_FOUND_OR_FORBIDDEN, Optional.empty(), false));
    when(ledger.recent(any(), eq(Optional.of("old")))).thenReturn(new LedgerResult.Failure<>(
        Code.STALE_CURSOR, Optional.empty(), false));
    when(ledger.recent(any(), eq(Optional.empty()))).thenReturn(new LedgerResult.Ok<>(
        new TransactionPage("recent", List.of(), 10, "0", Optional.empty())));
    var capture = capture();
    capture.handle(update(9, "/show 42"));
    capture.handle(update(10, "/recent old"));
    verify(ledger).recent(any(), eq(Optional.empty()));
  }

  @Test
  void credentialsAreNotRendered() {
    String token = "123:" + "secret".repeat(5);
    assertThat(new TelegramSettings(token, 77).toString()).doesNotContain(token);
    assertThatThrownBy(() -> new TelegramSettings(token, 0)).hasMessageNotContaining(token);
    assertThatThrownBy(() -> new TelegramSettings("", 77))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TelegramSettings(token, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TelegramSettings("99999999999999999999:" + "x".repeat(30), 77))
        .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("x".repeat(30));
  }
}
