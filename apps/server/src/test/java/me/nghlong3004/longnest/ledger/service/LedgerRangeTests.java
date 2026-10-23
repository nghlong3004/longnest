package me.nghlong3004.longnest.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Entry;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Totals;
import me.nghlong3004.longnest.ledger.service.impl.LedgerQueryServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

/** Synthetic port state, not SQL execution, transaction isolation or database-reopen proof. */
class LedgerRangeTests {
  private static final Instant NOW = Instant.parse("2026-10-21T00:00:00Z");
  private static final DateRange OCTOBER = DateRange.parse("2026-10-01", "2026-10-31");
  private static final HostContext HOST = new HostContext("personal", null, null, null);
  private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
  private final LedgerStore store = mock(LedgerStore.class);
  private final List<Entry> rows = new ArrayList<>();
  private final AtomicLong generation = new AtomicLong(7);
  private final LedgerCursors cursors = new LedgerCursors(Clock.fixed(NOW, ZoneOffset.UTC),
      new SecureRandom());
  private final LedgerQueryServiceImpl queries = new LedgerQueryServiceImpl(
      new PersonalOwner("personal"), store, new TransactionMapper(VALIDATORS.getValidator(),
          Mappers.getMapper(TransactionRepresentationMapper.class)), cursors);

  @AfterAll
  static void close() { VALIDATORS.close(); }

  @BeforeEach
  void data() {
    when(store.lockHead("personal")).thenAnswer(call -> generation.get());
    when(store.totals(eq("personal"), any(), any())).thenAnswer(call -> {
      var found = filtered(call.getArgument(1), call.getArgument(2));
      BigInteger income = BigInteger.ZERO, expense = BigInteger.ZERO;
      long incomeCount = 0, expenseCount = 0;
      for (Entry row : found) {
        if (row.intent().draft().type() == TransactionType.INCOME) {
          income = income.add(BigInteger.valueOf(row.intent().draft().amount().amountMinor()));
          incomeCount++;
        } else {
          expense = expense.add(BigInteger.valueOf(row.intent().draft().amount().amountMinor()));
          expenseCount++;
        }
      }
      return new Totals(income, BigInteger.valueOf(incomeCount), expense,
          BigInteger.valueOf(expenseCount));
    });
    when(store.range(eq("personal"), any(), any(), any())).thenAnswer(call -> {
      Optional<LedgerCursors.Key> key = call.getArgument(3);
      return filtered(call.getArgument(1), call.getArgument(2)).stream()
          .filter(row -> key.isEmpty() || row.intent().transactionDate().isBefore(key.get().date())
              || row.intent().transactionDate().equals(key.get().date()) && row.id() < key.get().id())
          .limit(11).toList();
    });
  }

  private List<Entry> filtered(DateRange range, RangeFilter filter) {
    return rows.stream().filter(row -> row.owner().equals("personal") && !row.deleted()
        && !row.intent().transactionDate().isBefore(range.from())
        && !row.intent().transactionDate().isAfter(range.to())
        && filter.includes(row.intent().draft().type(), row.intent().draft().category()))
        .sorted(Comparator.comparing((Entry row) -> row.intent().transactionDate())
            .thenComparingLong(Entry::id).reversed()).toList();
  }

  static Entry row(long id, String owner, TransactionType type, long amount, String date,
      boolean deleted) {
    return row(id, owner, type, Category.UNCATEGORIZED, amount, date, deleted);
  }

  static Entry row(long id, String owner, TransactionType type, Category category, long amount,
      String date, boolean deleted) {
    return new Entry(owner, id, 1, new CreateIntent(new TransactionDraft(type, new Money(amount),
        category, new Note("ghi chú " + id)), LocalDate.parse(date)),
        NOW.minusSeconds(id), Optional.empty(), deleted);
  }

  @ParameterizedTest
  @CsvSource({"1900-01-01,9999-12-31", "2026-10-20,2026-10-20"})
  void supportedBoundsAndEqualDayAreValid(String from, String to) {
    assertThat(DateRange.parse(from, to).from()).isEqualTo(LocalDate.parse(from));
  }

  @ParameterizedTest
  @CsvSource({"2026-02-30,2026-03-01", "2026-10-31,2026-10-01",
      "1899-12-31,2026-10-20", "2026-10-20,10000-01-01", "26-10-20,2026-10-21"})
  void invalidRangesNeverSubstituteDates(String from, String to) {
    assertThatThrownBy(() -> DateRange.parse(from, to)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void authorizationPrecedesInputOrStorage() {
    var emptyStore = mock(LedgerStore.class);
    var service = new LedgerQueryServiceImpl(new PersonalOwner("personal"), emptyStore,
        null, cursors);
    assertThatThrownBy(() -> service.list(new HostContext("foreign", null, null, null),
        null, null, null)).isInstanceOfSatisfying(LedgerException.class,
            error -> assertThat(error.getCode()).isEqualTo(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN));
    verifyNoInteractions(emptyStore);
  }

  @Test
  void fullTotalsAndTwoPagesUseInclusiveFinancialDateOrderingAndOwnerFilter() {
    rows.add(row(100, "personal", TransactionType.INCOME, 25_000_000, "2026-10-01", false));
    for (int id = 1; id <= 23; id++) {
      rows.add(row(id, "personal", TransactionType.EXPENSE, 40_000, "2026-10-31", false));
    }
    rows.add(row(201, "foreign", TransactionType.EXPENSE, 900_000, "2026-10-20", false));
    rows.add(row(202, "personal", TransactionType.EXPENSE, 900_000, "2026-10-20", true));
    rows.add(row(203, "personal", TransactionType.EXPENSE, 900_000, "2026-11-01", false));
    var first = queries.list(HOST, OCTOBER, RangeFilter.ALL, Optional.empty());
    assertThat(first.entries()).hasSize(10).extracting(view -> view.id())
        .containsExactly("23", "22", "21", "20", "19", "18", "17", "16", "15", "14");
    var total = first.summary().orElseThrow();
    assertThat(total.incomeTotal()).isEqualTo("25000000");
    assertThat(total.expenseTotal()).isEqualTo("920000");
    assertThat(total.expenseCount()).isEqualTo("23");
    assertThat(total.net()).isEqualTo("24080000");
    assertThat(total.generation()).isEqualTo(first.generation());
    var second = queries.list(HOST, OCTOBER, RangeFilter.ALL, first.nextCursor());
    assertThat(second.entries()).hasSize(10).first().extracting(view -> view.id()).isEqualTo("13");
    assertThat(second.summary()).contains(total);
    var third = queries.list(HOST, OCTOBER, RangeFilter.ALL, second.nextCursor());
    assertThat(third.entries()).hasSize(4);
    assertThat(third.nextCursor()).isEmpty();
    var order = inOrder(store);
    order.verify(store).lockHead("personal");
    order.verify(store).totals("personal", OCTOBER, RangeFilter.ALL);
    order.verify(store).range(eq("personal"), eq(OCTOBER), eq(RangeFilter.ALL), any());
  }

  @Test
  void typeSummaryEmptyNegativeAndHugeTotalsStayExact() {
    rows.add(row(1, "personal", TransactionType.EXPENSE, 40_000, "2026-10-20", false));
    var onlyExpense = queries.aggregate(HOST, OCTOBER, RangeFilter.EXPENSE);
    assertThat(onlyExpense.net()).isEqualTo("-40000");
    assertThat(onlyExpense.incomeCount()).isEqualTo("0");
    assertThat(queries.aggregate(HOST, OCTOBER, RangeFilter.INCOME).net()).isEqualTo("0");
    var huge = new BigInteger("922337203685477580800000");
    when(store.totals("personal", OCTOBER, RangeFilter.ALL))
        .thenReturn(new Totals(huge, BigInteger.TEN, BigInteger.ONE, BigInteger.ONE));
    assertThat(queries.aggregate(HOST, OCTOBER, RangeFilter.ALL).net())
        .isEqualTo(huge.subtract(BigInteger.ONE).toString());
  }

  @Test
  void foodTotalsAndPagesIncludeOnlyActiveOwnedFoodExpensesAcrossFullRange() {
    for (int id = 1; id <= 12; id++) {
      rows.add(row(id, "personal", TransactionType.EXPENSE, Category.FOOD, 40_000,
          "2026-10-20", false));
    }
    rows.add(row(20, "personal", TransactionType.EXPENSE, Category.GROCERIES, 900_000,
        "2026-10-20", false));
    rows.add(row(21, "personal", TransactionType.EXPENSE, Category.UNCATEGORIZED, 900_000,
        "2026-10-20", false));
    rows.add(row(22, "personal", TransactionType.INCOME, Category.SALARY, 900_000,
        "2026-10-20", false));
    rows.add(row(23, "personal", TransactionType.EXPENSE, Category.FOOD, 900_000,
        "2026-10-20", true));
    rows.add(row(24, "foreign", TransactionType.EXPENSE, Category.FOOD, 900_000,
        "2026-10-20", false));
    rows.add(row(25, "personal", TransactionType.EXPENSE, Category.FOOD, 900_000,
        "2026-11-01", false));

    var first = queries.list(HOST, OCTOBER, RangeFilter.FOOD, Optional.empty());
    assertThat(first.entries()).hasSize(10).extracting(view -> view.id())
        .containsExactly("12", "11", "10", "9", "8", "7", "6", "5", "4", "3");
    var summary = first.summary().orElseThrow();
    assertThat(summary.filter()).isEqualTo(RangeFilter.FOOD);
    assertThat(summary.incomeTotal()).isEqualTo("0");
    assertThat(summary.expenseTotal()).isEqualTo("480000");
    assertThat(summary.expenseCount()).isEqualTo("12");
    assertThat(summary.net()).isEqualTo("-480000");
    assertThat(queries.aggregate(HOST, OCTOBER, RangeFilter.FOOD)).isEqualTo(summary);
    assertThatThrownBy(() -> queries.list(HOST, OCTOBER, RangeFilter.EXPENSE,
        first.nextCursor())).isInstanceOfSatisfying(LedgerException.class,
            error -> assertThat(error.getCode()).isEqualTo(LedgerException.Code.INVALID_CURSOR));
    var second = queries.list(HOST, OCTOBER, RangeFilter.FOOD, first.nextCursor());
    assertThat(second.entries()).extracting(view -> view.id()).containsExactly("2", "1");
    assertThat(second.summary()).contains(summary);
    assertThat(second.nextCursor()).isEmpty();
  }

  @Test
  void foodPageRejectsWrongCategoryEvenIfStoreReturnsIt() {
    rows.add(row(1, "personal", TransactionType.EXPENSE, Category.FOOD, 1,
        "2026-10-20", false));
    when(store.range(eq("personal"), eq(OCTOBER), eq(RangeFilter.FOOD), any()))
        .thenReturn(List.of(row(2, "personal", TransactionType.EXPENSE, Category.GROCERIES, 1,
            "2026-10-20", false)));
    assertThatThrownBy(() -> queries.list(HOST, OCTOBER, RangeFilter.FOOD, Optional.empty()))
        .isInstanceOfSatisfying(LedgerException.class,
            error -> assertThat(error.getCode()).isEqualTo(LedgerException.Code.NOT_SAVED));
  }

  @Test
  void changedGenerationAndChangedQueryRejectOldCursorWithoutReinterpretingIt() {
    for (int id = 1; id <= 11; id++) {
      rows.add(row(id, "personal", TransactionType.EXPENSE, 1, "2026-10-20", false));
    }
    var first = queries.list(HOST, OCTOBER, RangeFilter.ALL, Optional.empty());
    assertThatThrownBy(() -> queries.list(HOST, OCTOBER, RangeFilter.EXPENSE, first.nextCursor()))
        .isInstanceOfSatisfying(LedgerException.class,
            error -> assertThat(error.getCode()).isEqualTo(LedgerException.Code.INVALID_CURSOR));
    generation.incrementAndGet();
    assertThatThrownBy(() -> queries.list(HOST, OCTOBER, RangeFilter.ALL, first.nextCursor()))
        .isInstanceOfSatisfying(LedgerException.class,
            error -> assertThat(error.getCode()).isEqualTo(LedgerException.Code.STALE_CURSOR));
    rows.removeFirst();
    var refreshed = queries.list(HOST, OCTOBER, RangeFilter.ALL, Optional.empty());
    assertThat(refreshed.summary().orElseThrow().expenseCount()).isEqualTo("10");
    assertThat(refreshed.generation()).isEqualTo("8");
  }

  @Test
  void correctionsDateChangesDeleteAndRestoreContributeOnlyCurrentActiveValues() {
    rows.add(row(1, "personal", TransactionType.INCOME, 25_000_000, "2026-10-20", false));
    rows.add(row(2, "personal", TransactionType.EXPENSE, 45_000, "2026-10-20", false));
    assertThat(queries.aggregate(HOST, OCTOBER, RangeFilter.ALL).net()).isEqualTo("24955000");
    rows.set(1, row(2, "personal", TransactionType.EXPENSE, 40_000, "2026-10-19", false));
    generation.incrementAndGet();
    assertThat(queries.aggregate(HOST, OCTOBER, RangeFilter.ALL).net()).isEqualTo("24960000");
    var day = DateRange.parse("2026-10-20", "2026-10-20");
    assertThat(queries.aggregate(HOST, day, RangeFilter.EXPENSE).expenseCount()).isEqualTo("0");
    rows.set(1, row(2, "personal", TransactionType.EXPENSE, 40_000, "2026-10-19", true));
    generation.incrementAndGet();
    assertThat(queries.aggregate(HOST, OCTOBER, RangeFilter.ALL).net()).isEqualTo("25000000");
    rows.set(1, row(2, "personal", TransactionType.EXPENSE, 40_000, "2026-10-19", false));
    generation.incrementAndGet();
    assertThat(queries.aggregate(HOST, OCTOBER, RangeFilter.ALL).net()).isEqualTo("24960000");
  }

  @Test
  void emptyRangeZerosAndMissingInputAndCanonicalZeroAreExplicit() {
    var page = queries.list(HOST, OCTOBER, RangeFilter.ALL, Optional.empty());
    assertThat(page.entries()).isEmpty();
    assertThat(page.nextCursor()).isEmpty();
    assertThat(page.summary().orElseThrow().net()).isEqualTo("0");
    assertThat(page.summary().orElseThrow().incomeCount()).isEqualTo("0");
    assertThatThrownBy(() -> DateRange.parse(null, "2026-10-20"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> queries.aggregate(HOST, null, RangeFilter.ALL))
        .isInstanceOfSatisfying(LedgerException.class,
            error -> assertThat(error.getCode()).isEqualTo(LedgerException.Code.INVALID_RANGE));
  }

  @Test
  void vietnamMidnightUsesFrozenFinancialDateNotLaterCreationTime() {
    var mapper = new TransactionMapper(VALIDATORS.getValidator(),
        Mappers.getMapper(TransactionRepresentationMapper.class));
    var input = new TransactionInput(TransactionType.EXPENSE, "1", "VND", Optional.empty(),
        Optional.empty(), Optional.empty());
    var before = mapper.normalize(input, Instant.parse("2026-10-19T16:59:59Z"));
    var after = mapper.normalize(input, Instant.parse("2026-10-19T17:00:00Z"));
    rows.add(new Entry("personal", 1, 1, before, NOW, Optional.empty(), false));
    rows.add(new Entry("personal", 2, 1, after, NOW, Optional.empty(), false));
    assertThat(queries.aggregate(HOST, DateRange.parse("2026-10-20", "2026-10-20"),
        RangeFilter.EXPENSE).expenseCount()).isEqualTo("1");
    assertThat(queries.list(HOST, DateRange.parse("2026-10-19", "2026-10-19"),
        RangeFilter.EXPENSE, Optional.empty()).entries()).singleElement()
        .satisfies(view -> assertThat(view.id()).isEqualTo("1"));
  }
}
