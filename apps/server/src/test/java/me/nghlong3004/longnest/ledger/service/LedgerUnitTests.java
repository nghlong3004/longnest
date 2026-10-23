package me.nghlong3004.longnest.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.service.impl.LedgerQueryServiceImpl;
import me.nghlong3004.longnest.ledger.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Mock-port unit evidence only: no transaction, database or durable replay claim. */
class LedgerUnitTests {
  private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
  private static final Instant REFERENCE = Instant.parse("2026-10-19T17:00:00.123456789Z");
  private static final Instant NOW = Instant.parse("2026-10-21T04:00:00.987654321Z");
  private static final HostContext HOST =
      new HostContext("personal", "event-1", REFERENCE, REFERENCE);
  private final PersonalOwner owner = new PersonalOwner("personal");
  private final TransactionMapper mapper = new TransactionMapper(
      VALIDATORS.getValidator(), Mappers.getMapper(TransactionRepresentationMapper.class));
  private final LedgerStore store = mock(LedgerStore.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final LedgerCursors cursors = new LedgerCursors(clock, new SecureRandom());
  private final TransactionServiceImpl transactions =
      new TransactionServiceImpl(owner, store, mapper, clock, new ControlProofs());
  private final LedgerQueryServiceImpl queries =
      new LedgerQueryServiceImpl(owner, store, mapper, cursors);

  static <T> T mock(Class<T> type) {
    return Mockito.mock(type);
  }

  @AfterAll
  static void closeValidator() {
    VALIDATORS.close();
  }

  @BeforeEach
  void emptyInbox() {
    when(store.inbox(any(), any())).thenReturn(Optional.empty());
    when(store.controlByRequest(any(), any())).thenReturn(Optional.empty());
    when(store.controlEventFingerprint(any(), any())).thenReturn(Optional.empty());
  }

  static TransactionInput input(String amount) {
    return new TransactionInput(TransactionType.EXPENSE, amount, "VND", Optional.empty(),
        Optional.of(Category.FOOD), Optional.of("ăn trưa"));
  }

  private LedgerStore.Inbox pending() {
    var intent = mapper.normalize(input("45000"), REFERENCE);
    return new LedgerStore.Inbox("personal", "event-1", mapper.fingerprint(intent), intent,
        TransactionMapper.micros(REFERENCE), Optional.of(TransactionMapper.micros(REFERENCE)),
        Optional.empty());
  }

  private static void fails(Runnable call, LedgerException.Code code) {
    assertThatThrownBy(call::run).isInstanceOfSatisfying(LedgerException.class,
        failure -> assertThat(failure.getCode()).isEqualTo(code));
  }

  @Test
  void absentFrozenCreateRecoveryDoesNotMutateOrHideStorageFailure() {
    assertThat(transactions.recoverCreateIfPresent(HOST)).isEmpty();
    verify(store, never()).insert(any(), any(), any(), any());
    verify(store, never()).freeze(any());
    when(store.lockHead("personal")).thenThrow(new LedgerException(LedgerException.Code.NOT_SAVED));
    fails(() -> transactions.recoverCreateIfPresent(HOST), LedgerException.Code.NOT_SAVED);
  }

  @Test
  void frozenReceiptRecoveryPrecedesReinterpretationAndNeverUsesCurrentEntry() {
    var frozen = pending();
    var completed = new LedgerStore.Inbox(frozen.owner(), frozen.operationId(),
        frozen.fingerprint(), frozen.intent(), frozen.businessReference(), frozen.messageAt(),
        Optional.of(new LedgerStore.Receipt(42, 9, TransactionMapper.micros(REFERENCE))));
    when(store.inbox("personal", "event-1")).thenReturn(Optional.of(completed));
    var recovered = transactions.recoverCreateIfPresent(HOST).orElseThrow();
    assertThat(recovered.replayed()).isTrue();
    assertThat(recovered.transaction().amountMinor()).isEqualTo("45000");
    verify(store, never()).insert(any(), any(), any(), any());
    verify(store, never()).entry(any(), anyLong());
  }

  @Test
  void frozenRecoveryRejectsForeignActorAndConflictingControlIdentity() {
    fails(() -> transactions.recoverCreateIfPresent(new HostContext("foreign", "event-1",
        REFERENCE, REFERENCE)), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    verifyNoInteractions(store);
    when(store.controlEventFingerprint("personal", "event-1")).thenReturn(Optional.of("control"));
    fails(() -> transactions.recoverCreateIfPresent(HOST), LedgerException.Code.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void everyServiceAuthorizesBeforeStoreOrInputInspection() {
    Mockito.reset(store);
    var foreign = new HostContext("foreign", "other", REFERENCE, null);
    fails(() -> transactions.ingest(foreign, null), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    fails(() -> transactions.create(foreign), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    fails(() -> transactions.initializeOwner(foreign),
        LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    fails(() -> queries.get(foreign, null), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    fails(() -> queries.recent(foreign, null), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    verifyNoInteractions(store);
  }

  @Test
  void initializationUsesOnlyConfiguredOwnerAndNoTransportIdentity() {
    transactions.initializeOwner(new HostContext("personal", null, null, null));
    verify(store).initializeOwner("personal");
    fails(() -> transactions.initializeOwner(HOST), LedgerException.Code.INVALID_FIELD);
    verify(store).initializeOwner("personal");
  }

  @Test
  void ingestionFreezesNormalizedIntentDateAndMicrosWithoutFinancialInsert() {
    transactions.ingest(HOST, input("45000"));
    var captured = ArgumentCaptor.forClass(LedgerStore.Inbox.class);
    var order = inOrder(store);
    order.verify(store).lockHead("personal");
    order.verify(store).inbox("personal", "event-1");
    order.verify(store).freeze(captured.capture());
    var frozen = captured.getValue();
    assertThat(frozen.intent().transactionDate()).isEqualTo(LocalDate.of(2026, 10, 20));
    assertThat(frozen.businessReference()).isEqualTo(Instant.parse("2026-10-19T17:00:00.123456Z"));
    assertThat(frozen.messageAt()).contains(frozen.businessReference());
    assertThat(frozen.receipt()).isEmpty();
    assertThat(frozen.fingerprint()).matches("[a-f0-9]{64}");
    verify(store, never()).insert(any(), any(), any(), any());
  }

  @Test
  void redeliveryUsesFrozenReferenceAcrossMidnightAndRejectsChangedPayload() {
    when(store.inbox("personal", "event-1")).thenReturn(Optional.of(pending()));
    var later = new HostContext("personal", "event-1", NOW, NOW);
    transactions.ingest(later, input("45000"));
    verify(store, never()).freeze(any());
    fails(() -> transactions.ingest(later, input("45001")),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void pendingRecoveryIsAuthorizedOwnerScopedAndLocksBeforeQuery() {
    var host = new HostContext("personal", null, null, null);
    when(store.pendingCreates("personal", "tg:123:")).thenReturn(List.of("tg:123:7"));
    assertThat(transactions.pendingCreates(host, "tg:123:")).containsExactly("tg:123:7");
    var order = inOrder(store);
    order.verify(store).lockHead("personal");
    order.verify(store).pendingCreates("personal", "tg:123:");
    fails(() -> transactions.pendingCreates(host, "tg:%:"), LedgerException.Code.INVALID_FIELD);
    fails(() -> transactions.pendingCreates(
        new HostContext("foreign", null, null, null), "tg:123:"),
        LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
  }

  @Test
  void createLocksBeforeReadingAndAppliesAllChangesInOneUnit() {
    var inbox = pending();
    when(store.lockHead("personal")).thenReturn(8L);
    when(store.inbox("personal", "event-1")).thenReturn(Optional.of(inbox));
    var entry = new LedgerStore.Entry("personal", 42, 1, inbox.intent(),
        TransactionMapper.micros(NOW), inbox.messageAt(), false);
    when(store.insert(eq("personal"), any(), any(), any())).thenReturn(entry);
    var receipt = transactions.create(new HostContext("personal", "event-1", null, null));
    var order = inOrder(store);
    order.verify(store).lockHead("personal");
    order.verify(store).inbox("personal", "event-1");
    order.verify(store).insert(
        "personal", inbox.intent(), TransactionMapper.micros(NOW), inbox.messageAt());
    order.verify(store).advance("personal", 8, 9);
    order.verify(store).complete(
        "personal", "event-1", new LedgerStore.Receipt(42, 9, entry.createdAt()));
    assertThat(receipt.generation()).isEqualTo("9");
    assertThat(receipt.transaction().id()).isEqualTo("42");
    assertThat(receipt.transaction().revision()).isEqualTo("1");
    assertThat(receipt.transaction().createdAt()).isEqualTo(TransactionMapper.micros(NOW));
    assertThat(receipt.replayed()).isFalse();
  }

  @Test
  void replayUsesOriginalReceiptEvenAtGenerationLimitAndNeverReadsCurrentEntry() {
    var frozen = pending();
    var receipt = new LedgerStore.Receipt(42, 9, TransactionMapper.micros(REFERENCE));
    var completed = new LedgerStore.Inbox(
        frozen.owner(), frozen.operationId(), frozen.fingerprint(),
        frozen.intent(), frozen.businessReference(), frozen.messageAt(), Optional.of(receipt));
    when(store.lockHead("personal")).thenReturn(Long.MAX_VALUE);
    when(store.inbox("personal", "event-1")).thenReturn(Optional.of(completed));
    var result = transactions.create(HOST);
    assertThat(result.replayed()).isTrue();
    assertThat(result.transaction().createdAt()).isEqualTo(receipt.createdAt());
    assertThat(result.transaction().amountMinor()).isEqualTo("45000");
    assertThat(result.generation()).isEqualTo("9");
    verify(store, never()).entry(any(), anyLong());
    verify(store, never()).insert(any(), any(), any(), any());
    verify(store, never()).advance(any(), anyLong(), anyLong());
    verify(store, never()).complete(any(), any(), any());
  }

  @Test
  void generationOverflowAndMissingInboxFailBeforeInsert() {
    when(store.lockHead("personal")).thenReturn(Long.MAX_VALUE);
    when(store.inbox("personal", "event-1")).thenReturn(Optional.of(pending()));
    fails(() -> transactions.create(HOST), LedgerException.Code.LIMIT_REACHED);
    when(store.inbox("personal", "event-1")).thenReturn(Optional.empty());
    fails(() -> transactions.create(HOST), LedgerException.Code.NOT_SAVED);
    verify(store, never()).insert(any(), any(), any(), any());
  }

  @Test
  void distinctEqualMessagesFreezeDistinctOperationsWithoutSuppression() {
    transactions.ingest(HOST, input("45000"));
    transactions.ingest(
        new HostContext("personal", "event-2", REFERENCE, REFERENCE), input("45000"));
    var captured = ArgumentCaptor.forClass(LedgerStore.Inbox.class);
    verify(store, Mockito.times(2)).freeze(captured.capture());
    assertThat(captured.getAllValues()).extracting(LedgerStore.Inbox::operationId)
        .containsExactly("event-1", "event-2");
    assertThat(captured.getAllValues()).extracting(LedgerStore.Inbox::fingerprint)
        .containsOnly(mapper.fingerprint(pending().intent()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "0", "0001", "-1", "1e6", "1.5tr", "1000000000001", "99999999999999999999"
  })
  void structuredInputRequiresCanonicalExactMagnitude(String amount) {
    fails(() -> mapper.normalize(input(amount), REFERENCE), LedgerException.Code.INVALID_AMOUNT);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1899-12-31", "10000-01-01", "2026-02-30", "2026-2-01", ""})
  void structuredDatesAreStrictAndBounded(String date) {
    var base = input("1");
    var explicit = new TransactionInput(base.type(), base.amountMinor(), base.currency(),
        Optional.of(date), base.category(), base.note());
    fails(() -> mapper.normalize(explicit, REFERENCE), LedgerException.Code.INVALID_DATE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1900-01-01", "9999-12-31", "2027-01-01"})
  void explicitDatesIncludeBoundariesAndFutureLabels(String date) {
    var explicit = new TransactionInput(TransactionType.INCOME, "1", "VND", Optional.of(date),
        Optional.empty(), Optional.empty());
    var intent = mapper.normalize(explicit, REFERENCE);
    assertThat(intent.transactionDate()).hasToString(date);
    assertThat(intent.draft().category()).isEqualTo(Category.UNCATEGORIZED);
    assertThat(intent.draft().note().value()).isEmpty();
  }

  @Test
  void validationRejectsNullOptionalsCurrencyCategoryAndUnsafeOrOverlongNotes() {
    fails(() -> mapper.normalize(new TransactionInput(TransactionType.EXPENSE, "1", "USD",
        Optional.empty(), Optional.empty(), Optional.empty()), REFERENCE),
        LedgerException.Code.INVALID_FIELD);
    fails(() -> mapper.normalize(new TransactionInput(TransactionType.EXPENSE, "1", "VND",
        null, Optional.empty(), Optional.empty()), REFERENCE), LedgerException.Code.INVALID_FIELD);
    for (String note : List.of("line\nbreak", "😀".repeat(257))) {
      fails(() -> mapper.normalize(new TransactionInput(TransactionType.EXPENSE, "1", "VND",
          Optional.empty(), Optional.empty(), Optional.of(note)), REFERENCE),
          LedgerException.Code.INVALID_FIELD);
    }
    fails(() -> mapper.normalize(new TransactionInput(TransactionType.EXPENSE, "1", "VND",
        Optional.empty(), Optional.of(Category.SALARY), Optional.empty()), REFERENCE),
        LedgerException.Code.INVALID_FIELD);
  }

  @Test
  void fingerprintUsesCanonicalNfcAndUnambiguousFields() {
    var composed = new TransactionInput(TransactionType.EXPENSE, "1", "VND", Optional.empty(),
        Optional.empty(), Optional.of("cà phê 42"));
    var decomposed = new TransactionInput(TransactionType.EXPENSE, "1", "VND", Optional.empty(),
        Optional.empty(), Optional.of("ca\u0300 phe\u0302 42"));
    assertThat(mapper.fingerprint(mapper.normalize(composed, REFERENCE)))
        .isEqualTo(mapper.fingerprint(mapper.normalize(decomposed, REFERENCE)));
    assertThat(mapper.fingerprint(mapper.normalize(input("1"), REFERENCE)))
        .isNotEqualTo(mapper.fingerprint(mapper.normalize(input("11"), REFERENCE)));
  }

  @Test
  void getReturnsOwnedDeletedStateAndMissingIdsRemainGeneric() {
    var entry = new LedgerStore.Entry("personal", 42, 3, pending().intent(),
        TransactionMapper.micros(NOW), Optional.empty(), true);
    when(store.entry("personal", 42)).thenReturn(Optional.of(entry));
    assertThat(queries.get(HOST, "42").state()).isEqualTo(TransactionView.State.DELETED);
    when(store.entry("personal", 42)).thenReturn(Optional.empty());
    fails(() -> queries.get(HOST, "42"), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    fails(() -> queries.get(HOST, "00"), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    fails(() -> queries.get(HOST, "9223372036854775808"),
        LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
  }

  @Test
  void recentUsesElevenRowsForTenVisibleAndCursorUsesSavedLastVisibleKey() {
    when(store.lockHead("personal")).thenReturn(12L);
    List<LedgerStore.Entry> found = IntStream.range(0, 11).mapToObj(index ->
        new LedgerStore.Entry("personal", 20 - index, 1, pending().intent(),
            TransactionMapper.micros(NOW), Optional.<Instant>empty(), false)).toList();
    when(store.recent("personal", Optional.empty())).thenReturn(found);
    var page = queries.recent(HOST, Optional.empty());
    assertThat(page.entries()).hasSize(10);
    assertThat(page.entries()).extracting(TransactionView::id)
        .containsExactly("20", "19", "18", "17", "16", "15", "14", "13", "12", "11");
    assertThat(cursors.resolve("personal", 12, page.nextCursor()))
        .contains(new LedgerCursors.Key(TransactionMapper.micros(NOW), 11));
    assertThat(page.generation()).isEqualTo("12");
    var order = inOrder(store);
    order.verify(store).lockHead("personal");
    order.verify(store).recent("personal", Optional.empty());
  }

  @Test
  void emptyRecentIsSuccessfulOnlyAfterEstablishedHeadLock() {
    when(store.recent("personal", Optional.empty())).thenReturn(List.of());
    var page = queries.recent(HOST, Optional.empty());
    assertThat(page.entries()).isEmpty();
    assertThat(page.nextCursor()).isEmpty();
    when(store.lockHead("personal")).thenThrow(new LedgerException(LedgerException.Code.NOT_SAVED));
    fails(() -> queries.recent(HOST, Optional.empty()), LedgerException.Code.NOT_SAVED);
  }

  @Test
  void queryBoundaryDoesNotLeakForeignOrDeletedRowsFromAnIncorrectAdapter() {
    var foreign = new LedgerStore.Entry("foreign", 42, 1, pending().intent(), NOW,
        Optional.empty(), false);
    when(store.entry("personal", 42)).thenReturn(Optional.of(foreign));
    fails(() -> queries.get(HOST, "42"), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    when(store.recent("personal", Optional.empty())).thenReturn(List.of(foreign));
    fails(() -> queries.recent(HOST, Optional.empty()), LedgerException.Code.NOT_SAVED);
    when(store.recent("personal", Optional.empty())).thenReturn(List.of(
        new LedgerStore.Entry("personal", 42, 1, pending().intent(), NOW, Optional.empty(), true)));
    fails(() -> queries.recent(HOST, Optional.empty()), LedgerException.Code.NOT_SAVED);
  }
}
