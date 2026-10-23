package me.nghlong3004.longnest.ledger.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.StandardEnvironment;

class TransactionMapperTests {
  private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
  private static final Instant SAVED = Instant.parse("2026-10-21T17:00:00.123456Z");
  private static final LocalDate DATE = LocalDate.of(2026, 10, 20);
  private final TransactionMapper mapper = new TransactionMapper(
      VALIDATORS.getValidator(), Mappers.getMapper(TransactionRepresentationMapper.class));

  @AfterAll
  static void closeValidator() {
    VALIDATORS.close();
  }

  private TransactionDraft draft(long amount) {
    return new TransactionDraft(TransactionType.EXPENSE, new Money(amount), Category.FOOD,
        new Note("ăn trưa 42"));
  }

  private LedgerStore.Entry entry(long amount, boolean deleted) {
    return new LedgerStore.Entry("personal", Long.MAX_VALUE, Long.MAX_VALUE,
        new CreateIntent(draft(amount), DATE), SAVED, Optional.of(SAVED), deleted);
  }

  @ParameterizedTest
  @ValueSource(longs = {1, 1_000_000_000_000L})
  void generatedInputAndViewPreserveEveryFieldAndExactBounds(long amount) {
    var draft = draft(amount);
    assertThat(mapper.input(draft)).isEqualTo(new TransactionInput(TransactionType.EXPENSE,
        Long.toString(amount), "VND", Optional.empty(), Optional.of(Category.FOOD),
        Optional.of("ăn trưa 42")));
    assertThat(mapper.view(entry(amount, false))).isEqualTo(new TransactionView(
        Long.toString(Long.MAX_VALUE), Long.toString(Long.MAX_VALUE), TransactionType.EXPENSE,
        Long.toString(amount), "VND", Category.FOOD, "ăn trưa 42", DATE, SAVED,
        Optional.of(SAVED), TransactionView.State.ACTIVE));
  }

  @Test
  void generatedViewPreservesDeletedAndAbsentSourceTime() {
    var active = entry(45_000, false);
    var deleted = new LedgerStore.Entry(active.owner(), active.id(), active.revision(),
        active.intent(), active.createdAt(), Optional.empty(), true);
    var view = mapper.view(deleted);
    assertThat(view.state()).isEqualTo(TransactionView.State.DELETED);
    assertThat(view.messageAt()).isEmpty();
    assertThat(view.transactionDate()).isEqualTo(DATE);
  }

  @Test
  void generatedReceiptStillUsesFrozenOriginalNotCurrentState() {
    var frozen = new LedgerStore.Inbox("personal", "event-1", "fingerprint",
        new CreateIntent(draft(45_000), DATE), SAVED, Optional.empty(),
        Optional.of(new LedgerStore.Receipt(42, Long.MAX_VALUE, SAVED)));
    var result = mapper.receipt(frozen, true);
    assertThat(result.operationId()).isEqualTo("event-1");
    assertThat(result.operation()).isEqualTo("create");
    assertThat(result.changed()).isTrue();
    assertThat(result.replayed()).isTrue();
    assertThat(result.generation()).isEqualTo(Long.toString(Long.MAX_VALUE));
    assertThat(result.transaction()).isEqualTo(new TransactionView("42", "1",
        TransactionType.EXPENSE, "45000", "VND", Category.FOOD, "ăn trưa 42", DATE,
        SAVED, Optional.empty(), TransactionView.State.ACTIVE));
  }

  @Test
  void generatedPagePreservesOrderCursorAndImmutablePageInvariants() {
    var entries = new ArrayList<>(List.of(entry(1, false), entry(2, false)));
    var page = mapper.page(entries, Long.MAX_VALUE, Optional.of("opaque"));
    assertThat(page.entries())
        .containsExactly(mapper.view(entries.get(0)), mapper.view(entries.get(1)));
    assertThat(page.scope()).isEqualTo("recent");
    assertThat(page.pageSize()).isEqualTo(10);
    assertThat(page.generation()).isEqualTo(Long.toString(Long.MAX_VALUE));
    assertThat(page.nextCursor()).contains("opaque");
    entries.clear();
    assertThat(page.entries()).hasSize(2);
    assertThatThrownBy(() -> page.entries().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(mapper.page(List.of(), 0, Optional.empty()).entries()).isEmpty();
    assertThatThrownBy(() -> mapper.page(java.util.Collections.nCopies(11, entry(1, false)),
        0, Optional.empty())).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> mapper.input(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> mapper.view(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void realSpringScanInjectsGeneratedMapperOnlyUnderLedgerProfile() {
    try (var inactive = context(false)) {
      assertThat(inactive.getBeansOfType(TransactionMapper.class)).isEmpty();
      assertThat(inactive.getBeansOfType(TransactionRepresentationMapper.class)).isEmpty();
    }
    try (var active = context(true)) {
      assertThat(active.getBeansOfType(TransactionRepresentationMapper.class)).hasSize(1);
      assertThat(active.getBean(TransactionRepresentationMapper.class).getClass().getSimpleName())
          .isEqualTo("TransactionRepresentationMapperImpl");
      var injected = active.getBean(TransactionMapper.class);
      assertThat(injected.input(draft(45_000))).isEqualTo(mapper.input(draft(45_000)));
      var normalized = injected.normalize(injected.input(draft(45_000)), SAVED);
      assertThat(normalized.transactionDate()).isEqualTo(LocalDate.of(2026, 10, 22));
      assertThatThrownBy(() -> injected.normalize(null, SAVED))
          .isInstanceOfSatisfying(LedgerException.class,
              failure -> assertThat(failure.getCode())
                  .isEqualTo(LedgerException.Code.INVALID_FIELD));
    }
  }

  private AnnotationConfigApplicationContext context(boolean active) {
    var context = new AnnotationConfigApplicationContext();
    var environment = new StandardEnvironment();
    environment.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    environment.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment.setActiveProfiles(active ? "ledger" : "unit");
    context.setEnvironment(environment);
    context.registerBean(Validator.class, VALIDATORS::getValidator);
    context.scan("me.nghlong3004.longnest.ledger.mapper");
    context.refresh();
    return context;
  }
}
