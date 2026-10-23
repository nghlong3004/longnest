package me.nghlong3004.longnest.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.OperationRecovery;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionPatch;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.model.LedgerException.Code;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.AsyncTaskExecutor;
import tools.jackson.databind.json.JsonMapper;

/** Actual ChatClient, memory advisor, schema and native manager; no provider or database. */
class ConversationAssistantTests {
  private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
  private final TransactionMapper mapper = new TransactionMapper(validators.getValidator(),
      Mappers.getMapper(TransactionRepresentationMapper.class));
  private final ChatModel model = mock(ChatModel.class);
  private final LedgerClient ledger = mock(LedgerClient.class);
  private final MutableClock clock = new MutableClock();
  private final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory());
  private final ConversationAssistant assistant = new ConversationAssistant(model,
      new PersonalOwner("personal"), ledger, clock, executor, new LedgerTools(ledger, mapper),
      JsonMapper.builder().build());

  ConversationAssistantTests() {
    when(model.getOptions()).thenReturn(DeepSeekChatOptions.builder().build());
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Ok<>(Optional.empty()));
    when(ledger.list(any(), any(), any(), any())).thenReturn(new LedgerResult.Ok<>(
        new TransactionPage("recent", List.of(), 10, "0", Optional.empty(), Optional.empty())));
  }

  @AfterEach void close() { executor.close(); validators.close(); }

  private HostContext host(String event) { return host(event, "tg:123:77"); }
  private HostContext host(String event, String scope) {
    return new HostContext("personal", event, clock.instant(), clock.instant(), scope);
  }

  static ChatResponse tool(String name, String arguments) {
    return new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("")
        .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", name, arguments)))
        .build(), ChatGenerationMetadata.builder().finishReason("tool_calls").build())));
  }

  private void select(String name, String arguments) {
    when(model.call(any(Prompt.class))).thenReturn(tool(name, arguments));
  }

  @Test void relativeQueriesAndFollowupsUseFrozenVietnamDateAndValidatedDimensions() {
    select("list_transactions", "{\"period\":\"YESTERDAY\",\"scope\":\"EXPENSE\"}");
    assertThat(assistant.reply(host("1"), "hôm qua anh chi bao nhiêu?").result()).isNotNull();
    verify(ledger).list(any(), eq(DateRange.parse("2026-10-22", "2026-10-22")),
        eq(RangeFilter.EXPENSE), eq(Optional.empty()));
    select("list_transactions", "{\"period\":\"THIS_MONTH\",\"scope\":\"KEEP\"}");
    assistant.reply(host("2"), "tháng này thì sao?");
    verify(ledger).list(any(), eq(DateRange.parse("2026-10-01", "2026-10-23")),
        eq(RangeFilter.EXPENSE), eq(Optional.empty()));
    select("list_transactions", "{\"period\":\"KEEP\",\"scope\":\"FOOD\"}");
    assistant.reply(host("3"), "chỉ tiền ăn thôi");
    verify(ledger).list(any(), eq(DateRange.parse("2026-10-01", "2026-10-23")),
        eq(RangeFilter.FOOD), eq(Optional.empty()));
    var prompts = ArgumentCaptor.forClass(Prompt.class);
    verify(model, times(3)).call(prompts.capture());
    assertThat(prompts.getAllValues().getLast().getInstructions())
        .filteredOn(m -> m.getMessageType() == MessageType.USER).hasSize(3);
    assertThat(prompts.getAllValues().getLast().getSystemMessage().getText())
        .contains("2026-10-23", "EXPENSE", "2026-10-01");
    assertThat(prompts.getAllValues().getLast().getInstructions())
        .noneMatch(m -> m.getMessageType() == MessageType.TOOL || m.getMessageType() == MessageType.ASSISTANT);
    verify(ledger, never()).create(any(), any());
  }

  @Test void missingScopeForeignIdentityExpiryAndResetNeverReuseAnotherConversation() {
    select("list_transactions", "{\"period\":\"TODAY\",\"scope\":\"EXPENSE\"}");
    assistant.reply(host("1"), "hôm nay chi bao nhiêu?");
    clearInvocations(ledger);
    select("list_transactions", "{\"period\":\"KEEP\",\"scope\":\"FOOD\"}");
    assertThat(assistant.reply(host("2", "tg:456:77"), "chỉ tiền ăn thôi").messageKey()).isNotNull();
    assistant.reset(host("reset"));
    assertThat(assistant.reply(host("3"), "chỉ tiền ăn thôi").messageKey()).isNotNull();
    verify(ledger, never()).list(any(), any(), any(), any());
    assertThatThrownBy(() -> assistant.reply(new HostContext("foreign", "x", clock.instant(),
        null, "tg:123:77"), "x")).isInstanceOf(RuntimeException.class);
    verify(model, times(3)).call(any(Prompt.class));
  }

  @Test void explicitFoodScopeChangeKeepsPreviousIncomeDatesWithoutContradiction() {
    select("list_transactions", "{\"period\":\"THIS_MONTH\",\"scope\":\"INCOME\"}");
    assistant.reply(host("1"), "thu nhập tháng này");
    select("list_transactions", "{\"period\":\"KEEP\",\"scope\":\"FOOD\"}");
    assertThat(assistant.reply(host("2"), "Còn chi tiền ăn trong cùng khoảng đó").result()).isNotNull();
    verify(ledger).list(any(), eq(DateRange.parse("2026-10-01", "2026-10-23")),
        eq(RangeFilter.FOOD), eq(Optional.empty()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"period\":\"TODAY\",\"scope\":\"OWNER\"}",
      "{\"period\":\"TODAY\",\"scope\":\"ALL\",\"owner\":\"foreign\"}",
      "{\"period\":\"TODAY\",\"period\":\"KEEP\",\"scope\":\"ALL\"}",
      "{\"period\":\"TODAY\",\"scope\":\"ALL\"} {}", "null", "[]",
      "{\"period\":null,\"scope\":\"ALL\"}",
      "{\"period\":\"EXPLICIT\",\"scope\":\"ALL\",\"from\":\"2026-02-30\",\"to\":\"2026-03-01\"}"})
  void invalidNativeArgumentsNeverDispatch(String json) {
    select("list_transactions", json);
    assertThat(assistant.reply(host("1"), "xem giao dịch").result()).isNull();
    verify(ledger, never()).list(any(), any(), any(), any());
    verify(ledger, never()).create(any(), any());
  }

  @Test void multipleCallsUnknownToolsAndModelNarratedSuccessNeverExecute() {
    var one = tool("create_transaction", "{}");
    var output = AssistantMessage.builder().content("").toolCalls(List.of(
        one.getResult().getOutput().getToolCalls().getFirst(),
        one.getResult().getOutput().getToolCalls().getFirst())).build();
    when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(output,
        ChatGenerationMetadata.builder().finishReason("tool_calls").build()))));
    assistant.reply(host("1"), "mua đồ");
    select("confirm_delete", "{\"confirmed\":true}");
    assistant.reply(host("2"), "đã xác nhận");
    when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
        new AssistantMessage("Đã lưu 45000 VND"),
        ChatGenerationMetadata.builder().finishReason("stop").build()))));
    assertThat(assistant.reply(host("3"), "mua đồ").result()).isNull();
    verify(ledger, never()).create(any(), any());
    verify(ledger, never()).confirmDelete(any(), any(), any());
  }

  @Test void nativeCaptureUsesLiteralAmountAndHostDateNotModelMoneyArithmetic() {
    select("create_transaction", """
        {"type":"EXPENSE","amount":"45 nghìn","currency":"VND","category":"FOOD",
         "note":"bữa trưa","period":"YESTERDAY"}
        """);
    var receipt = receipt();
    when(ledger.create(any(), any())).thenReturn(new LedgerResult.Ok<>(receipt));
    assertThat(assistant.reply(host("1"), "Hôm qua mình trả 45 nghìn cho bữa trưa.").result())
        .isEqualTo(new LedgerResult.Ok<>(receipt));
    verify(ledger).create(any(), eq(new TransactionInput(TransactionType.EXPENSE, "45000", "VND",
        Optional.of("2026-10-22"), Optional.of(Category.FOOD), Optional.of("bữa trưa"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"45", "45000", "0", "45k USD", "45.000"})
  void inventedOrPartialAmountNeverCreates(String amount) {
    select("create_transaction", """
        {"type":"EXPENSE","amount":"%s","currency":"VND","category":"FOOD",
         "note":"bữa trưa","period":"TODAY"}
        """.formatted(amount));
    assistant.reply(host("1"), "Mình trả 45 nghìn cho bữa trưa.");
    verify(ledger, never()).create(any(), any());
  }

  @Test void recoveredReceiptAndUnavailableRecoveryPrecedeAnyModelRequest() {
    var original = receipt();
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Ok<>(Optional.of(
        new OperationRecovery(original, null))));
    assertThat(assistant.reply(host("1"), "wording changed").result())
        .isEqualTo(new LedgerResult.Ok<>(original));
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Failure<>(
        Code.OUTCOME_UNKNOWN,
        Optional.of("1"), true));
    assertThat(assistant.reply(host("1"), "wording changed").result()).isInstanceOf(LedgerResult.Failure.class);
    verifyNoInteractions(model);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Mình vay 45k bữa trưa", "hoàn tiền 45k bữa trưa",
      "Chuyển khoản 45k bữa trưa", "Mình trả 45k USD bữa trưa", "Mình trả 45k bữa trưa?"})
  void knownUnsupportedFlowOrCurrencyCannotBeRelabeledByTool(String text) {
    select("create_transaction", """
        {"type":"EXPENSE","amount":"45k","currency":"VND","category":"FOOD",
         "note":"bữa trưa","period":"TODAY"}
        """);
    assistant.reply(host("1"), text);
    verify(ledger, never()).create(any(), any());
  }

  @Test void requestQuotaIsBoundedAndResetsOnlyAfterAnHour() {
    select("clarify", "{\"missing\":\"AMOUNT\"}");
    for (int index = 0; index < 60; index++) assistant.reply(host("event-" + index), "ăn trưa");
    assertThat(assistant.reply(host("limited"), "ăn trưa").messageKey()).isEqualTo("conversation.limit");
    verify(model, times(60)).call(any(Prompt.class));
    clock.now = clock.now.plusSeconds(3600);
    assertThat(assistant.reply(host("next-hour"), "ăn trưa").messageKey()).isEqualTo("conversation.AMOUNT");
    verify(model, times(61)).call(any(Prompt.class));
  }

  @Test void interruptedModelCannotExecuteCallbackAndConcurrentTurnIsBusy() throws Exception {
    var entered = new CountDownLatch(1);
    var finished = new CountDownLatch(1);
    when(model.call(any(Prompt.class))).thenAnswer(call -> {
      entered.countDown();
      try { new CountDownLatch(1).await(); }
      catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
      finally { finished.countDown(); }
      return tool("create_transaction", "{}");
    });
    var result = new AtomicReference<ConversationAssistant.Reply>();
    Thread caller = Thread.ofVirtual().start(() -> result.set(assistant.reply(host("1"), "mua đồ")));
    assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(assistant.reply(host("2"), "mua đồ").messageKey()).isEqualTo("conversation.busy");
    caller.interrupt();
    caller.join(2000);
    assertThat(caller.isAlive()).isFalse();
    assertThat(result.get().messageKey()).isEqualTo("conversation.unavailable");
    assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue();
    verify(ledger, never()).create(any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void cancellationBeforeExecutorStartsDoesNotPermanentlyWedgeConversation() throws Exception {
    var delayed = mock(AsyncTaskExecutor.class);
    var enqueued = new CountDownLatch(1);
    when(delayed.submit(any(Callable.class))).thenAnswer(call -> {
      var pending = new FutureTask<>((Callable<ChatResponse>) call.getArgument(0));
      enqueued.countDown();
      return pending;
    });
    var waiting = new ConversationAssistant(model, new PersonalOwner("personal"), ledger, clock,
        delayed, new LedgerTools(ledger, mapper), JsonMapper.builder().build());
    var first = new AtomicReference<ConversationAssistant.Reply>();
    Thread caller = Thread.ofVirtual().start(() -> first.set(waiting.reply(host("1"), "ăn trưa")));
    assertThat(enqueued.await(2, TimeUnit.SECONDS)).isTrue();
    caller.interrupt();
    caller.join(2000);
    assertThat(caller.isAlive()).isFalse();
    assertThat(first.get().messageKey()).isEqualTo("conversation.unavailable");
    when(delayed.submit(any(Callable.class))).thenAnswer(call ->
        executor.submit((Callable<ChatResponse>) call.getArgument(0)));
    select("clarify", "{\"missing\":\"AMOUNT\"}");
    assertThat(waiting.reply(host("2"), "ăn trưa").messageKey()).isEqualTo("conversation.AMOUNT");
    verify(model).call(any(Prompt.class));
  }

  @Test void selectedTargetCanOnlyPreviewOrRequestDeleteAndRestoreUsesHostRevision() {
    assistant.selected(host("select"), receipt().transaction());
    select("preview_update", "{\"id\":\"\",\"field\":\"AMOUNT\",\"value\":\"40k\",\"period\":\"TODAY\"}");
    when(ledger.previewUpdate(any(), any(), any(), any(), any())).thenReturn(
        new LedgerResult.Failure<>(Code.INVALID_STATE,
            Optional.empty(), false));
    assistant.reply(host("1"), "sửa thành 40k");
    verify(ledger).previewUpdate(any(), eq("42"), eq("3"), eq(new TransactionPatch(
        Optional.of("40000"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())), eq(null));
    select("request_delete_transaction", "{\"id\":\"\"}");
    assistant.reply(host("2"), "xóa khoản này");
    verify(ledger).requestDeleteCurrent(any(), eq("42"));
    select("restore_transaction", "{\"id\":\"\"}");
    assistant.reply(host("3"), "khôi phục khoản này");
    verify(ledger).restore(any(), eq("42"), eq("3"));
    verify(ledger, never()).update(any(), any(), any());
    verify(ledger, never()).confirmDelete(any(), any(), any());
  }

  private MutationReceipt receipt() {
    return new MutationReceipt("1", "create", true, true, new TransactionView("42", "3",
        TransactionType.EXPENSE, "45000", "VND", Category.FOOD, "private ledger note",
        LocalDate.parse("2026-10-22"), clock.instant(), Optional.empty(), TransactionView.State.ACTIVE), "3");
  }

  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-10-22T18:00:00Z");
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    @Override public Instant instant() { return now; }
  }
}
