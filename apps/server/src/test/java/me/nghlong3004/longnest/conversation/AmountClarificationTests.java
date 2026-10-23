package me.nghlong3004.longnest.conversation;

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
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.OperationRecovery;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.LedgerException.Code;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import tools.jackson.databind.json.JsonMapper;

class AmountClarificationTests {
  private static final Instant START = Instant.parse("2026-10-23T16:59:00Z");
  private final AtomicReference<Instant> now = new AtomicReference<>(START);
  private final Clock clock = mock(Clock.class);
  private final ChatModel model = mock(ChatModel.class);
  private final LedgerClient ledger = mock(LedgerClient.class);
  private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
  private final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory());
  private final ConversationAssistant assistant;

  AmountClarificationTests() {
    when(clock.instant()).thenAnswer(ignored -> now.get());
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Ok<>(Optional.empty()));
    when(ledger.create(any(), any())).thenReturn(new LedgerResult.Failure<>(
        Code.NOT_SAVED, Optional.empty(), true));
    var mapper = new TransactionMapper(validators.getValidator(),
        Mappers.getMapper(TransactionRepresentationMapper.class));
    assistant = new ConversationAssistant(model, new PersonalOwner("personal"), ledger, clock,
        executor, new LedgerTools(ledger, mapper), JsonMapper.builder().build());
  }

  @AfterEach void close() { executor.close(); validators.close(); }

  private HostContext host(String event) { return host(event, "tg:123:77"); }
  private HostContext host(String event, String scope) {
    return new HostContext("personal", event, now.get(), now.get(), scope);
  }

  private ConversationAssistant.Reply text(String event, String text) {
    return assistant.amountCapture(host(event), text, true, null).orElseThrow();
  }

  @Test void freezesSourceFactsAndOriginalVietnamDateWithoutModelOrImplicitRescaling() {
    assertThat(text("source", "ăn trưa").messageKey()).isEqualTo("conversation.AMOUNT");
    now.set(START.plusSeconds(120)); // midnight crossed, but still the original financial date
    text("amount", "45k");
    verify(ledger).create(eq(host("amount")), eq(new TransactionInput(TransactionType.EXPENSE,
        "45000", "VND", Optional.of("2026-10-23"), Optional.of(Category.FOOD), Optional.of("ăn trưa"))));
    assertThat(assistant.amountCapture(host("next"), "50k", true, null)).isEmpty();
    assertThat(text("source", "ăn trưa").messageKey()).isEqualTo("conversation.captureAgain");
    verify(ledger, times(1)).create(any(), any());
    verifyNoInteractions(model);
  }

  @Test void incomeAndBareDongKeepTheirValidatedMeanings() {
    text("source", "nhận lương");
    text("amount", "45");
    verify(ledger).create(any(), eq(new TransactionInput(TransactionType.INCOME, "45", "VND",
        Optional.of("2026-10-23"), Optional.of(Category.SALARY), Optional.of("nhận lương"))));
    verifyNoInteractions(model);
  }

  @ParameterizedTest
  @CsvSource({"45 nghìn,45000", "45 ngàn,45000", "0.05 triệu,50000"})
  void pendingCaptureReusesExistingNativeMagnitudeVocabularyWithoutInference(String amount,
      String expected) {
    text("source", "ăn trưa");
    text("amount", amount);
    verify(ledger).create(any(), eq(new TransactionInput(TransactionType.EXPENSE, expected, "VND",
        Optional.of("2026-10-23"), Optional.of(Category.FOOD), Optional.of("ăn trưa"))));
    verifyNoInteractions(model);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "45.000", "45,000", "45 k", "45k USD", "-45k", "+45k",
      "1e6", "1000000000001", "45k 50k"})
  void invalidAmountLikeReplyKeepsSourceButNeverWritesOrUsesModel(String invalid) {
    text("source", "ăn trưa");
    assertThat(text("invalid", invalid).messageKey()).isEqualTo("conversation.AMOUNT");
    verify(ledger, never()).create(any(), any());
    text("valid", "45k");
    verify(ledger).create(any(), any());
    verifyNoInteractions(model);
  }

  @ParameterizedTest
  @ValueSource(strings = {"xem hôm qua chi bao nhiêu?", "ăn tối 60k", "-60k đồ dùng", "không nữa",
      "45 nghìn cho bữa trưa", "45000 bữa trưa"})
  void unrelatedRequestOrCompleteCaptureAbandonsPendingAndContinuesExistingRoute(String unrelated) {
    text("source", "ăn trưa");
    assertThat(assistant.amountCapture(host("other"), unrelated, true, null)).isEmpty();
    assertThat(assistant.amountCapture(host("amount"), "45k", true, null)).isEmpty();
    verify(ledger, never()).create(any(), any());
    verifyNoInteractions(model);
  }

  @Test void newSourceReplacesOldAndAnOldDeliveryCannotRearmIt() {
    text("lunch", "ăn trưa");
    text("salary", "nhận lương");
    assertThat(text("lunch", "ăn trưa").messageKey()).isEqualTo("conversation.captureAgain");
    text("amount", "25tr");
    verify(ledger).create(any(), eq(new TransactionInput(TransactionType.INCOME, "25000000", "VND",
        Optional.of("2026-10-23"), Optional.of(Category.SALARY), Optional.of("nhận lương"))));
  }

  @Test void expiryIsNotExtendedByInvalidRepliesAndResetOrControlRoutingDiscardsPending() {
    text("source", "ăn trưa");
    now.set(START.plusSeconds(599));
    text("invalid", "0");
    now.set(START.plusSeconds(600));
    assertThat(assistant.amountCapture(host("late"), "45k", true, null)).isEmpty();
    text("new", "ăn tối");
    assistant.reset(host("reset"));
    assertThat(assistant.amountCapture(host("after-reset"), "45k", true, null)).isEmpty();
    text("newer", "ăn trưa");
    assertThat(assistant.amountCapture(host("field-or-command"), "45k", false, null)).isEmpty();
    assertThat(assistant.amountCapture(host("after-control"), "45k", true, null)).isEmpty();
    verify(ledger, never()).create(any(), any());
  }

  @Test void identitiesAreIsolatedAndForeignPrincipalCannotTouchPending() {
    text("source", "ăn trưa");
    assertThat(assistant.amountCapture(host("foreign-chat", "tg:456:77"), "45k", true, null)).isEmpty();
    assertThatThrownBy(() -> assistant.amountCapture(new HostContext("foreign", "x", START,
        START, "tg:123:77"), "45k", true, null)).isInstanceOf(LedgerException.class);
    text("amount", "45k");
    verify(ledger).create(eq(host("amount")), any());
    verifyNoInteractions(model);
  }

  @Test void sameFailedEventUsesFrozenAttemptButAnotherEventCannotConsumeIt() {
    text("source", "ăn trưa");
    text("amount", "45k");
    text("amount", "99k"); // retry cannot alter the already chosen input, even before ingest succeeds
    verify(ledger, times(2)).create(any(), eq(new TransactionInput(TransactionType.EXPENSE,
        "45000", "VND", Optional.of("2026-10-23"), Optional.of(Category.FOOD), Optional.of("ăn trưa"))));
    assertThat(assistant.amountCapture(host("different"), "99k", true, null)).isEmpty();
    verify(ledger, times(2)).create(any(), any());
  }

  @Test void durableRecoveryPrecedesEphemeralFactsAndStorageFailureStopsCompletion() {
    text("source", "ăn trưa");
    var failure = new LedgerResult.Failure<Optional<OperationRecovery>>(
        Code.OUTCOME_UNKNOWN, Optional.of("amount"), true);
    when(ledger.recoverOperation(eq(host("amount")))).thenReturn(failure);
    assertThat(text("amount", "45k").result()).isEqualTo(failure);
    verify(ledger, never()).create(any(), any());
    var receipt = new MutationReceipt("amount", "create", true, true,
        new TransactionView("42", "1", TransactionType.EXPENSE, "45000", "VND",
            Category.FOOD, "ăn trưa", LocalDate.parse("2026-10-23"), START,
            Optional.of(START), TransactionView.State.ACTIVE), "1");
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Ok<>(
        Optional.of(new OperationRecovery(receipt, null))));
    assistant.reset(host("reset"));
    assertThat(assistant.reply(host("amount"), "99k").result()).isEqualTo(new LedgerResult.Ok<>(receipt));
    verify(ledger, never()).create(any(), any());
    verifyNoInteractions(model);
  }

  @Test void sourceReplayDoesNotExtendPendingLifetimeAndSourceTrackingIsBounded() {
    text("source", "ăn trưa");
    now.set(START.plusSeconds(599));
    assertThat(text("source", "ăn trưa").messageKey()).isEqualTo("conversation.AMOUNT");
    now.set(START.plusSeconds(600));
    assertThat(assistant.amountCapture(host("amount"), "45k", true, null)).isEmpty();
    for (int i = 0; i < 7; i++) text("new-" + i, "ăn trưa");
    assertThat(text("over-bound", "ăn trưa").messageKey()).isEqualTo("conversation.captureAgain");
    assertThat(assistant.amountCapture(host("over-bound-amount"), "45k", true, null)).isEmpty();
    verify(ledger, never()).create(any(), any());
  }

  @Test void existingContextCapacityEvictionAlsoForgetsPendingSourceFacts() {
    text("source", "ăn trưa");
    for (int i = 0; i < 16; i++) {
      assistant.amountCapture(host("source-" + i, "tg:bot-" + i + ":77"), "ăn trưa", true, null);
    }
    assertThat(assistant.amountCapture(host("after-eviction"), "45k", true, null)).isEmpty();
    verify(ledger, never()).create(any(), any());
    verifyNoInteractions(model);
  }
}
