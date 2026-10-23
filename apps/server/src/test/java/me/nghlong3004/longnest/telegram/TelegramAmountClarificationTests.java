package me.nghlong3004.longnest.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import me.nghlong3004.longnest.conversation.ConversationAssistant;
import me.nghlong3004.longnest.conversation.LedgerTools;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.service.LedgerQueryService;
import me.nghlong3004.longnest.ledger.service.impl.TransactionServiceImpl;
import me.nghlong3004.longnest.presentation.UiMessagesTests;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramFailure;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import tools.jackson.databind.json.JsonMapper;

/** Real capture/services/mapper/rendering over a mock storage port, never a DB or Telegram call. */
class TelegramAmountClarificationTests {
  private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final PersonalOwner owner = new PersonalOwner("personal");
  private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
  private final TransactionMapper mapper = new TransactionMapper(validators.getValidator(),
      Mappers.getMapper(TransactionRepresentationMapper.class));
  private final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory());
  private final LedgerStore store = mock(LedgerStore.class);
  private final ChatModel model = mock(ChatModel.class);
  private final TelegramClient client = mock(TelegramClient.class);
  private final Map<String, LedgerStore.Inbox> inboxes = new HashMap<>();
  private final List<LedgerStore.Entry> entries = new ArrayList<>();
  private final List<SendMessage> sends = new ArrayList<>();
  private final AtomicBoolean failDelivery = new AtomicBoolean();
  private final LedgerClient ledger = new LedgerClient(owner,
      new TransactionServiceImpl(owner, store, mapper, clock, new ControlProofs()),
      mock(LedgerQueryService.class));
  private TelegramCapture capture;
  private ConversationAssistant conversation;

  @BeforeEach void setup() throws Exception {
    when(model.getOptions()).thenReturn(DeepSeekChatOptions.builder().build());
    when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
        AssistantMessage.builder().content("").toolCalls(List.of(new AssistantMessage.ToolCall(
            "clarify", "function", "clarify", "{\"missing\":\"TYPE\"}"))).build(),
        ChatGenerationMetadata.builder().finishReason("tool_calls").build()))));
    when(store.inbox(any(), any())).thenAnswer(call -> Optional.ofNullable(inboxes.get(call.getArgument(1))));
    doAnswer(call -> {
      LedgerStore.Inbox frozen = call.getArgument(0);
      inboxes.put(frozen.operationId(), frozen);
      return null;
    }).when(store).freeze(any());
    when(store.insert(any(), any(), any(), any())).thenAnswer(call -> {
      var entry = new LedgerStore.Entry(call.getArgument(0), 42 + entries.size(), 1,
          call.getArgument(1), call.getArgument(2), call.getArgument(3), false);
      entries.add(entry);
      return entry;
    });
    doAnswer(call -> {
      String event = call.getArgument(1);
      LedgerStore.Inbox frozen = inboxes.get(event);
      inboxes.put(event, new LedgerStore.Inbox(frozen.owner(), event, frozen.fingerprint(),
          frozen.intent(), frozen.businessReference(), frozen.messageAt(),
          Optional.of(call.getArgument(2))));
      return null;
    }).when(store).complete(any(), any(), any());
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      if (failDelivery.getAndSet(false)) throw new TelegramApiException("synthetic delivery failure");
      sends.add(call.getArgument(0));
      var sent = new Message();
      sent.setMessageId(100 + sends.size());
      sent.setChat(new Chat(77L, "private"));
      return sent;
    });
    capture = new TelegramCapture(client, new TelegramSettings("123:" + "x".repeat(30), 77),
        owner, new TransactionInterpreter(), mapper, ledger,
        new TelegramReplies(UiMessagesTests.vietnamese()), clock,
        new TelegramNavigation(clock, new SecureRandom()), new ControlProofs());
    restartConversation();
  }

  private void restartConversation() {
    conversation = new ConversationAssistant(model, owner, ledger, clock, executor,
        new LedgerTools(ledger, mapper), JsonMapper.builder().build());
    capture.setConversation(conversation);
  }

  @AfterEach void close() { executor.close(); validators.close(); }

  private Update event(int id, String text) {
    var message = new Message();
    message.setFrom(new User(77L, "Minh", false));
    message.setChat(new Chat(77L, "private"));
    message.setMessageId(id);
    message.setDate(Math.toIntExact(NOW.getEpochSecond()));
    message.setText(text);
    var update = new Update();
    update.setUpdateId(id);
    update.setMessage(message);
    return update;
  }

  @Test void amountReplySavesOriginalFactsAndRendersAuthoritativeReceiptWithExistingControls() {
    capture.handle(event(1, "ăn trưa"));
    assertThat(sends.getLast().getText()).contains("Khoản này bao nhiêu tiền?");
    assertThat(entries).isEmpty();
    capture.handle(event(2, "45k"));
    assertThat(entries).hasSize(1);
    var intent = entries.getFirst().intent();
    assertThat(intent.draft().note().value()).isEqualTo("ăn trưa");
    assertThat(intent.draft().type()).isEqualTo(TransactionType.EXPENSE);
    assertThat(intent.draft().category()).isEqualTo(Category.FOOD);
    assertThat(intent.draft().amount().canonicalAmount()).isEqualTo("45000");
    assertThat(intent.transactionDate().toString()).isEqualTo("2026-10-23");
    assertThat(inboxes.get("tg:123:2").receipt()).isPresent();
    assertThat(sends.getLast().getText()).contains("Đã lưu", "#42", "45000 VND", "ăn trưa");
    assertThat(sends.getLast().getReplyMarkup()).isNotNull();
    assertThat(sends.getLast().getParseMode()).isNull();
    verifyNoInteractions(model);
    capture.handle(event(3, "45k"));
    assertThat(entries).hasSize(1);
    assertThat(sends.getLast().getText()).contains("khoản thu hay khoản chi");
    verify(model).call(any(Prompt.class));
  }

  @Test void failedReceiptDeliveryRecoversSameEventAfterConversationRestartWithoutSecondInsert() {
    capture.handle(event(1, "ăn trưa"));
    failDelivery.set(true);
    assertThatThrownBy(() -> capture.handle(event(2, "45k"))).isInstanceOf(TelegramFailure.class);
    assertThat(entries).hasSize(1);
    restartConversation();
    capture.handle(event(2, "99k"));
    assertThat(entries).hasSize(1);
    assertThat(sends.getLast().getText()).contains("phát lại", "45000 VND", "ăn trưa");
    verify(store, times(1)).insert(any(), any(), any(), any());
    verifyNoInteractions(model);
  }

  @Test void restartBeforeAmountReplyForgetsUnsavedSourceAndRoutesMissingTypeToModel() {
    capture.handle(event(1, "ăn trưa"));
    restartConversation();
    capture.handle(event(2, "45k"));
    assertThat(entries).isEmpty();
    assertThat(sends.getLast().getText()).contains("khoản thu hay khoản chi");
    verify(model).call(any(Prompt.class));
  }

  @Test void newCompleteCaptureSavesImmediatelyAndDoesNotUseOldPendingDescription() {
    capture.handle(event(1, "ăn trưa"));
    capture.handle(event(2, "-60k đồ dùng"));
    assertThat(entries).hasSize(1);
    assertThat(entries.getFirst().intent().draft().note().value()).isEqualTo("đồ dùng");
    capture.handle(event(3, "45k"));
    assertThat(entries).hasSize(1);
    verify(model).call(any(Prompt.class));
  }

  @Test void helpAndFieldReplyCannotAccidentallyCompletePendingCapture() {
    capture.handle(event(1, "ăn trưa"));
    capture.handle(event(2, "/help"));
    capture.handle(event(3, "45k"));
    capture.handle(event(4, "ăn trưa"));
    var fieldReply = event(5, "45k");
    var prompt = new Message();
    prompt.setMessageId(999);
    fieldReply.getMessage().setReplyToMessage(prompt);
    capture.handle(fieldReply);
    capture.handle(event(6, "45k"));
    assertThat(entries).isEmpty();
    verify(store, never()).insert(any(), any(), any(), any());
    verify(model, times(2)).call(any(Prompt.class));
  }

  @Test void foreignTelegramIdentityCannotCompleteOrCancelAuthenticatedPendingCapture() {
    capture.handle(event(1, "ăn trưa"));
    var foreign = event(2, "45k");
    foreign.getMessage().getFrom().setId(88L);
    capture.handle(foreign);
    assertThat(entries).isEmpty();
    capture.handle(event(3, "45k"));
    assertThat(entries).hasSize(1);
    verifyNoInteractions(model);
  }

  @Test void replyToOldQuestionCannotConsumeReplacementButReplyToCurrentQuestionCan() {
    capture.handle(event(1, "ăn trưa"));
    var oldQuestion = new Message(); oldQuestion.setMessageId(101);
    capture.handle(event(2, "nhận lương"));
    var currentQuestion = new Message(); currentQuestion.setMessageId(102);
    var stale = event(3, "45k"); stale.getMessage().setReplyToMessage(oldQuestion);
    capture.handle(stale);
    assertThat(entries).isEmpty();
    assertThat(sends.getLast().getText()).contains("không còn là câu hỏi hiện tại");
    var valid = event(4, "25tr"); valid.getMessage().setReplyToMessage(currentQuestion);
    capture.handle(valid);
    assertThat(entries).hasSize(1);
    assertThat(entries.getFirst().intent().draft().note().value()).isEqualTo("nhận lương");
    assertThat(entries.getFirst().intent().draft().type()).isEqualTo(TransactionType.INCOME);
    assertThat(entries.getFirst().intent().draft().amount().canonicalAmount()).isEqualTo("25000000");
    verifyNoInteractions(model);
  }

  @Test void directCaptureAndCommandsStillRunWhileCancelledModelTaskIsUnwinding() throws Exception {
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var finished = new CountDownLatch(1);
    when(model.getOptions()).thenReturn(DeepSeekChatOptions.builder().build());
    when(model.call(any(Prompt.class))).thenAnswer(call -> {
      entered.countDown();
      try {
        boolean done = false;
        while (!done) {
          try { release.await(); done = true; }
          catch (InterruptedException ignored) { /* Simulate a provider that ignores cancellation. */ }
        }
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall("call", "function", "clarify",
                "{\"missing\":\"DATES\"}"))).build(),
            ChatGenerationMetadata.builder().finishReason("tool_calls").build())));
      } finally { finished.countDown(); }
    });
    var queries = mock(LedgerQueryService.class);
    when(queries.recent(any(), any())).thenReturn(new TransactionPage("recent", List.of(), 10,
        "0", Optional.empty(), Optional.empty()));
    // Reuse the same real transaction service but a known read-only recent result.
    var busyLedger = new LedgerClient(owner,
        new TransactionServiceImpl(owner, store, mapper, clock, new ControlProofs()), queries);
    capture = new TelegramCapture(client, new TelegramSettings("123:" + "x".repeat(30), 77),
        owner, new TransactionInterpreter(), mapper, busyLedger,
        new TelegramReplies(UiMessagesTests.vietnamese()), clock,
        new TelegramNavigation(clock, new SecureRandom()), new ControlProofs());
    conversation = new ConversationAssistant(model, owner, busyLedger, clock, executor,
        new LedgerTools(busyLedger, mapper), JsonMapper.builder().build());
    capture.setConversation(conversation);
    var running = conversation;
    Thread caller = Thread.ofVirtual().start(() -> running.reply(
        new HostContext("personal", "model", NOW, NOW,
            "tg:123:77"), "xem các khoản chi"));
    try {
      assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
      caller.interrupt(); caller.join(2000);
      assertThat(caller.isAlive()).isFalse();
      capture.handle(event(1, "ăn trưa 45k"));
      assertThat(entries).hasSize(1);
      capture.handle(event(2, "/help"));
      capture.handle(event(3, "/recent"));
      capture.handle(event(4, "/reset"));
      assertThat(sends).hasSize(4);
      assertThat(sends.getLast().getText()).contains("Đã xóa ngữ cảnh");
      verify(queries).recent(any(), any());
      assertThat(sends).noneMatch(s -> s.getText().contains("đang xử lý yêu cầu trước"));
    } finally {
      release.countDown();
      assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue();
    }
  }
}
