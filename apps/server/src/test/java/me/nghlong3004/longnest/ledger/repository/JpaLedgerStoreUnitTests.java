package me.nghlong3004.longnest.ledger.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.entity.CreateInbox;
import me.nghlong3004.longnest.ledger.model.entity.LedgerEntry;
import me.nghlong3004.longnest.ledger.model.entity.OwnerHead;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.util.ReflectionTestUtils;

/** Repository calls, real entity mapping and SQL-source checks only; no provider/database. */
class JpaLedgerStoreUnitTests {
  private static final Instant NOW = Instant.parse("2026-10-21T04:00:00.123456Z");
  private final OwnerHeadRepository heads = mock(OwnerHeadRepository.class);
  private final EntryRepository entries = mock(EntryRepository.class);
  private final InboxRepository inboxes = mock(InboxRepository.class);
  private final ControlRepository controls = mock(ControlRepository.class);
  private final ControlEventRepository controlEvents = mock(ControlEventRepository.class);
  private final EntityManager entityManager = mock(EntityManager.class);
  private final JpaLedgerStore store = new JpaLedgerStore(heads, entries, inboxes, controls,
      controlEvents, entityManager);

  private Query durableInitQueries() {
    var lock = mock(Query.class);
    var durability = mock(Query.class);
    when(entityManager.createNativeQuery(
        "lock table ledger_owner_head, ledger_entry, ledger_create_inbox, ledger_control, ledger_control_event in exclusive mode"))
        .thenReturn(lock);
    when(entityManager.createNativeQuery(
        "select current_setting('fsync') || ':' || current_setting('synchronous_commit')"))
        .thenReturn(durability);
    when(durability.getSingleResult()).thenReturn("on:on");
    return lock;
  }

  @Test
  void devPreparationCreatesOnlyFreshStorageThenAcceptsItsSoleOwnerWithoutReset() {
    var lock = durableInitQueries();
    when(heads.count()).thenReturn(0L);
    store.prepareOwner("personal");
    var saved = ArgumentCaptor.forClass(OwnerHead.class);
    verify(heads).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getGeneration()).isZero();
    when(heads.count()).thenReturn(1L);
    when(heads.existsById("personal")).thenReturn(true);
    when(entries.count()).thenReturn(12L);
    store.prepareOwner("personal");
    verify(lock, times(2)).executeUpdate();
    verify(heads, times(1)).saveAndFlush(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6})
  void devPreparationRejectsForeignMultipleOrMissingHeadsWithNonemptyStorage(int occupied) {
    durableInitQueries();
    if (occupied == 1) when(heads.count()).thenReturn(1L); // wrong owner
    if (occupied == 2) when(heads.count()).thenReturn(2L);
    if (occupied == 3) when(entries.count()).thenReturn(1L);
    if (occupied == 4) when(inboxes.count()).thenReturn(1L);
    if (occupied == 5) when(controls.count()).thenReturn(1L);
    if (occupied == 6) when(controlEvents.count()).thenReturn(1L);
    assertThatThrownBy(() -> store.prepareOwner("personal")).isInstanceOf(LedgerException.class);
    verify(heads, never()).saveAndFlush(any());
  }

  @Test
  void devPreparationDoesNotConvertQueryFailureIntoFreshStorage() {
    durableInitQueries();
    when(heads.count()).thenThrow(new IllegalStateException("unavailable"));
    assertThatThrownBy(() -> store.prepareOwner("personal")).hasMessage("unavailable");
    verify(heads, never()).saveAndFlush(any());
  }

  @Test
  void devPreparationRequiresDurabilityEvenForAnExistingOwner() {
    durableInitQueries();
    when(entityManager.createNativeQuery(
        "select current_setting('fsync') || ':' || current_setting('synchronous_commit')")
        .getSingleResult()).thenReturn("on:off");
    assertThatThrownBy(() -> store.prepareOwner("personal")).isInstanceOf(LedgerException.class);
    verifyNoInteractions(heads);
  }

  @Test
  void nativeNumericProjectionNeverNarrowsTotalsOrCounts() {
    var range = DateRange.parse("1900-01-01", "9999-12-31");
    var projection = mock(EntryRepository.TotalsProjection.class);
    when(entries.totals("personal", range.from(), range.to(), "ALL")).thenReturn(projection);
    when(projection.getIncomeTotal()).thenReturn(new BigDecimal("92233720368547758080000"));
    when(projection.getExpenseTotal()).thenReturn(new BigDecimal("1"));
    when(projection.getIncomeCount()).thenReturn(new BigDecimal("9223372036854775808"));
    when(projection.getExpenseCount()).thenReturn(BigDecimal.ONE);
    var result = store.totals("personal", range, RangeFilter.ALL);
    assertThat(result.incomeCount().toString()).isEqualTo("9223372036854775808");
    assertThat(result.net().toString()).isEqualTo("92233720368547758079999");
    when(entries.totals("personal", range.from(), range.to(), "FOOD")).thenReturn(projection);
    when(projection.getIncomeTotal()).thenReturn(BigDecimal.ZERO);
    when(projection.getIncomeCount()).thenReturn(BigDecimal.ZERO);
    assertThat(store.totals("personal", range, RangeFilter.FOOD).net().toString())
        .isEqualTo("-1");
    verify(entries).totals("personal", range.from(), range.to(), "FOOD");
    when(projection.getIncomeTotal()).thenReturn(new BigDecimal("1.5"));
    assertThatThrownBy(() -> store.totals("personal", range, RangeFilter.ALL))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void rangeQueryBindingUsesInclusiveDatesAndElevenKeysetRows() throws Exception {
    var range = DateRange.parse("2026-10-01", "2026-10-31");
    when(entries.range("personal", range.from(), range.to(), "EXPENSE", range.to(), 42,
        PageRequest.of(0, 11))).thenReturn(List.of());
    assertThat(store.range("personal", range, RangeFilter.EXPENSE,
        Optional.of(new LedgerCursors.Key(null, 42, range.to())))).isEmpty();
    verify(entries).range("personal", range.from(), range.to(), "EXPENSE", range.to(), 42,
        PageRequest.of(0, 11));
    assertThat(store.range("personal", range, RangeFilter.FOOD,
        Optional.of(new LedgerCursors.Key(null, 42, range.to())))).isEmpty();
    verify(entries).range("personal", range.from(), range.to(), "FOOD", range.to(), 42,
        PageRequest.of(0, 11));
    // Simple-name collision with jakarta.persistence.Query used for the EntityManager fixtures.
    var query = EntryRepository.class.getMethod("totals", String.class, LocalDate.class,
        LocalDate.class, String.class).getAnnotation(org.springframework.data.jpa.repository.Query.class);
    assertThat(query.nativeQuery()).isTrue();
    assertThat(query.value()).contains("sum(amount_minor)", "as numeric", "deleted = false",
        "transaction_date between :fromDate and :toDate", "owner_id = :owner");
    assertThat(query.value()).contains(
        ":typeFilter = 'FOOD' and transaction_type = 'EXPENSE' and category = 'FOOD'");
    var pageQuery = EntryRepository.class.getMethod("range", String.class, LocalDate.class,
        LocalDate.class, String.class, LocalDate.class, long.class,
        org.springframework.data.domain.Pageable.class)
        .getAnnotation(org.springframework.data.jpa.repository.Query.class);
    assertThat(pageQuery.nativeQuery()).isFalse();
    assertThat(pageQuery.value()).contains("e.owner = :owner", "e.deleted = false",
        "e.values.transactionDate between :fromDate and :toDate",
        ":typeFilter = 'FOOD' and cast(e.values.type as string) = 'EXPENSE'",
        "cast(e.values.category as string) = 'FOOD'",
        "order by e.values.transactionDate desc, e.id desc");
  }

  @Test
  void initializationLocksAllTablesChecksDurabilityAndPersistsGenerationZero() {
    var lock = durableInitQueries();
    store.initializeOwner("personal");
    var saved = ArgumentCaptor.forClass(OwnerHead.class);
    var order = inOrder(entityManager, heads, entries, inboxes, controls, controlEvents);
    order.verify(entityManager).createNativeQuery(
        "lock table ledger_owner_head, ledger_entry, ledger_create_inbox, ledger_control, ledger_control_event in exclusive mode");
    verify(lock).executeUpdate();
    order.verify(entityManager).createNativeQuery(
        "select current_setting('fsync') || ':' || current_setting('synchronous_commit')");
    order.verify(heads).count();
    order.verify(entries).count();
    order.verify(inboxes).count();
    order.verify(controls).count();
    order.verify(controlEvents).count();
    order.verify(heads).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getOwner()).isEqualTo("personal");
    assertThat(saved.getValue().getGeneration()).isZero();
    assertThat(saved.getValue().getRowVersion()).isNull();
    assertThatThrownBy(() -> new OwnerHead(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  void initializationRejectsAnyExistingHeadEntryOrInbox(int occupiedTable) {
    durableInitQueries();
    if (occupiedTable == 1) when(heads.count()).thenReturn(1L);
    if (occupiedTable == 2) when(entries.count()).thenReturn(1L);
    if (occupiedTable == 3) when(inboxes.count()).thenReturn(1L);
    if (occupiedTable == 4) when(controls.count()).thenReturn(1L);
    if (occupiedTable == 5) when(controlEvents.count()).thenReturn(1L);
    assertThatThrownBy(() -> store.initializeOwner("personal"))
        .isInstanceOf(LedgerException.class);
    verify(heads, never()).saveAndFlush(any());
  }

  @Test
  void initializationRejectsDowngradedDurabilityAndUniqueKeyRace() {
    durableInitQueries();
    var query = mock(Query.class);
    when(entityManager.createNativeQuery(
        "select current_setting('fsync') || ':' || current_setting('synchronous_commit')"))
        .thenReturn(query);
    when(query.getSingleResult()).thenReturn("on:off");
    assertThatThrownBy(() -> store.initializeOwner("personal"))
        .isInstanceOf(LedgerException.class);
    verify(heads, never()).count();

    when(query.getSingleResult()).thenReturn("on:on");
    doThrow(new DataIntegrityViolationException("synthetic PK race"))
        .when(heads).saveAndFlush(any());
    assertThatThrownBy(() -> store.initializeOwner("personal"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static CreateIntent intent() {
    return new CreateIntent(new TransactionDraft(TransactionType.EXPENSE, new Money(45000),
        Category.FOOD, new Note("ăn trưa")), LocalDate.of(2026, 10, 20));
  }

  private static OwnerHead newOwnerHead() {
    try {
      var constructor = OwnerHead.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (ReflectiveOperationException failure) {
      throw new AssertionError(failure);
    }
  }

  private static LedgerStore.Inbox pending() {
    return new LedgerStore.Inbox("personal", "event-1", "a".repeat(64), intent(), NOW,
        Optional.of(NOW), Optional.empty());
  }

  @Test
  void pendingRecoveryQueryIsLimitedToTenAndOwnerNamespaceBound() {
    when(inboxes.pending("personal", "tg:123:", PageRequest.of(0, 10)))
        .thenReturn(List.of("tg:123:7"));
    assertThat(store.pendingCreates("personal", "tg:123:")).containsExactly("tg:123:7");
    verify(inboxes).pending("personal", "tg:123:", PageRequest.of(0, 10));
  }

  @Test
  void missingEstablishedHeadDoesNotSeedOrReadOtherTables() {
    when(heads.lockOwner("personal")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> store.lockHead("personal"))
        .isInstanceOfSatisfying(LedgerException.class,
            failure -> assertThat(failure.getCode()).isEqualTo(LedgerException.Code.NOT_SAVED));
    verifyNoInteractions(entries, inboxes, entityManager);
  }

  @Test
  void headIsLockedBeforeDurabilitySettingsAreRead() {
    var head = newOwnerHead();
    ReflectionTestUtils.setField(head, "generation", 7L);
    when(heads.lockOwner("personal")).thenReturn(Optional.of(head));
    var query = mock(Query.class);
    when(entityManager.createNativeQuery(any(String.class))).thenReturn(query);
    when(query.getSingleResult()).thenReturn("on:on");
    assertThat(store.lockHead("personal")).isEqualTo(7);
    var order = inOrder(heads, entityManager, query);
    order.verify(heads).lockOwner("personal");
    order.verify(entityManager).createNativeQuery(
        "select current_setting('fsync') || ':' || current_setting('synchronous_commit')");
    order.verify(query).getSingleResult();
  }

  @ParameterizedTest
  @ValueSource(strings = {"off:on", "on:off", "on:local", "off:off"})
  void durabilityDowngradesFailClosed(String settings) {
    when(heads.lockOwner("personal")).thenReturn(Optional.of(newOwnerHead()));
    var query = mock(Query.class);
    when(entityManager.createNativeQuery(any(String.class))).thenReturn(query);
    when(query.getSingleResult()).thenReturn(settings);
    assertThatThrownBy(() -> store.lockHead("personal")).isInstanceOf(LedgerException.class);
  }

  @Test
  void inboxFreezesRealValuesAndOriginalReceiptCannotBeReplaced() {
    store.freeze(pending());
    var captured = ArgumentCaptor.forClass(CreateInbox.class);
    verify(inboxes).save(captured.capture());
    var entity = captured.getValue();
    assertThat(entity.snapshot()).isEqualTo(pending());
    assertThat(entity.getRowVersion()).isNull();
    var receipt = new LedgerStore.Receipt(42, 1, NOW);
    entity.complete(receipt);
    assertThat(entity.snapshot().receipt()).contains(receipt);
    assertThat(entity.snapshot().intent()).isEqualTo(pending().intent());
    assertThatThrownBy(() -> entity.complete(new LedgerStore.Receipt(43, 2, NOW)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void insertFlushesEntityAndReturnsOnlyAnImmutableSnapshot() {
    when(entries.saveAndFlush(any())).thenAnswer(call -> {
      LedgerEntry entity = call.getArgument(0);
      ReflectionTestUtils.setField(entity, "id", 42L);
      return entity;
    });
    var saved = store.insert("personal", intent(), NOW, Optional.of(NOW));
    assertThat(saved).isEqualTo(new LedgerStore.Entry("personal", 42, 1, intent(), NOW,
        Optional.of(NOW), false));
    assertThat(saved.intent().draft().amount()).isEqualTo(new Money(45000));
  }

  @Test
  void headAdvancementDoesNotManuallyChangeProviderVersion() {
    var head = newOwnerHead();
    ReflectionTestUtils.setField(head, "generation", 7L);
    ReflectionTestUtils.setField(head, "rowVersion", 100L);
    when(heads.findById("personal")).thenReturn(Optional.of(head));
    store.advance("personal", 7, 8);
    assertThat(head.getGeneration()).isEqualTo(8);
    assertThat(head.getRowVersion()).isEqualTo(100);
    assertThatThrownBy(() -> store.advance("personal", 7, 8))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void ownedGetAndKeysetQueriesDoNotReturnManagedEntities() {
    var entity = new LedgerEntry("personal", intent(), NOW, Optional.empty());
    ReflectionTestUtils.setField(entity, "id", 42L);
    when(entries.findByOwnerAndId("personal", 42)).thenReturn(Optional.of(entity));
    assertThat(store.entry("personal", 42)).contains(entity.snapshot());
    when(entries.findTop11ByOwnerAndDeletedFalseOrderByCreatedAtDescIdDesc("personal"))
        .thenReturn(List.of(entity));
    assertThat(store.recent("personal", Optional.empty())).containsExactly(entity.snapshot());
    when(entries.after("personal", NOW, 43, PageRequest.of(0, 11))).thenReturn(List.of(entity));
    assertThat(store.recent("personal", Optional.of(new LedgerCursors.Key(NOW, 43))))
        .containsExactly(entity.snapshot());
    verify(entries).after("personal", NOW, 43, PageRequest.of(0, 11));
  }

  @Test
  void metadataAndMigrationSourceDeclareInvariantsWithoutExecutingSql() throws Exception {
    var lock = OwnerHeadRepository.class.getMethod("lockOwner", String.class)
        .getAnnotation(Lock.class);
    assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    for (Class<?> type : List.of(OwnerHead.class, LedgerEntry.class, CreateInbox.class)) {
      assertThat(type.isRecord()).isFalse();
      assertThat(Modifier.isFinal(type.getModifiers())).isFalse();
      assertThat(Modifier.isProtected(type.getDeclaredConstructor().getModifiers())).isTrue();
      assertThat(type.getDeclaredField("rowVersion").getAnnotation(Version.class)).isNotNull();
    }
    var generator = LedgerEntry.class.getDeclaredField("id").getAnnotation(SequenceGenerator.class);
    assertThat(generator.allocationSize()).isEqualTo(1);
    try (var stream = getClass().getResourceAsStream("/db/ledger/V1__create_ledger.sql")) {
      assertThat(stream).isNotNull();
      String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(sql).contains("NO CYCLE", "PRIMARY KEY (owner_id, operation_id)",
          "WHERE deleted = FALSE", "note IS NFC NORMALIZED", "receipt_entry_id IS NOT NULL");
      assertThat(sql.toUpperCase(java.util.Locale.ROOT))
          .doesNotContain("INSERT INTO", "CREATE DATABASE");
    }
  }
}
