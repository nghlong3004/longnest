package me.nghlong3004.longnest.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.input.InputInterpreter;
import me.nghlong3004.longnest.input.DeepSeekInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Entry;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Totals;
import me.nghlong3004.longnest.ledger.service.TransactionService;
import me.nghlong3004.longnest.ledger.service.impl.LedgerQueryServiceImpl;
import me.nghlong3004.longnest.presentation.UiMessagesTests;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation.RangeSelection;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation.Route;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import tools.jackson.databind.json.JsonMapper;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Actual query service/adapter over synthetic state; no database or Telegram transport request. */
class TelegramRangeTests {
  private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
  private final MutableClock clock = new MutableClock();
  private final LedgerStore store = mock(LedgerStore.class);
  private final TelegramClient client = mock(TelegramClient.class);
  private final TransactionService transactions = mock(TransactionService.class);
  private final List<Entry> rows = new ArrayList<>();
  private final List<SendMessage> sends = new ArrayList<>();
  private final List<Message> delivered = new ArrayList<>();
  private final AtomicLong generation = new AtomicLong(1);
  private final PersonalOwner owner = new PersonalOwner("personal");
  private final TransactionMapper mapper = new TransactionMapper(VALIDATORS.getValidator(),
      Mappers.getMapper(TransactionRepresentationMapper.class));
  private TelegramCapture capture;
  private int event = 1;

  @AfterAll
  static void close() { VALIDATORS.close(); }

  private List<Entry> found(DateRange range, RangeFilter filter) {
    return rows.stream().filter(row -> !row.deleted() && row.owner().equals("personal")
        && !row.intent().transactionDate().isBefore(range.from())
        && !row.intent().transactionDate().isAfter(range.to())
        && filter.includes(row.intent().draft().type()))
        .sorted(Comparator.comparing((Entry row) -> row.intent().transactionDate())
            .thenComparingLong(Entry::id).reversed()).toList();
  }

  @BeforeEach
  void setup() throws Exception {
    for (int id = 1; id <= 23; id++) rows.add(row(id, TransactionType.EXPENSE, 40000));
    rows.add(row(100, TransactionType.INCOME, 25000000));
    when(store.lockHead("personal")).thenAnswer(call -> generation.get());
    when(store.entry(eq("personal"), anyLong())).thenAnswer(call -> rows.stream()
        .filter(row -> row.id() == (long) call.getArgument(1)).findFirst());
    when(store.totals(eq("personal"), any(), any())).thenAnswer(call -> {
      var result = found(call.getArgument(1), call.getArgument(2));
      BigInteger income = BigInteger.ZERO, expense = BigInteger.ZERO;
      long inCount = 0, outCount = 0;
      for (var row : result) {
        var amount = BigInteger.valueOf(row.intent().draft().amount().amountMinor());
        if (row.intent().draft().type() == TransactionType.INCOME) {
          income = income.add(amount); inCount++;
        } else { expense = expense.add(amount); outCount++; }
      }
      return new Totals(income, BigInteger.valueOf(inCount), expense, BigInteger.valueOf(outCount));
    });
    when(store.range(eq("personal"), any(), any(), any())).thenAnswer(call -> {
      Optional<LedgerCursors.Key> key = call.getArgument(3);
      return found(call.getArgument(1), call.getArgument(2)).stream().filter(row -> key.isEmpty()
          || row.intent().transactionDate().isBefore(key.get().date())
          || row.intent().transactionDate().equals(key.get().date()) && row.id() < key.get().id())
          .limit(11).toList();
    });
    when(client.execute(any(AnswerCallbackQuery.class))).thenReturn(true);
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      SendMessage sent = call.getArgument(0); sends.add(sent);
      var message = new Message(); message.setMessageId(100 + sends.size());
      message.setChat(new Chat(77L, "private")); delivered.add(message); return message;
    });
    restart();
  }

  private Entry row(long id, TransactionType type, long amount) {
    return new Entry("personal", id, 1,
        new CreateIntent(new TransactionDraft(type, new Money(amount), Category.UNCATEGORIZED,
            new Note("ghi chú " + id)), LocalDate.parse("2026-10-20")),
        clock.instant(), Optional.empty(), false);
  }

  private void restart() {
    restart(new TransactionInterpreter());
  }

  private void restart(InputInterpreter interpreter) {
    var queries = new LedgerQueryServiceImpl(owner, store, mapper,
        new LedgerCursors(clock, new SecureRandom()));
    capture = new TelegramCapture(client, new TelegramSettings("123:" + "x".repeat(30), 77),
        owner, interpreter, mapper,
        new LedgerClient(owner, transactions, queries),
        new TelegramReplies(UiMessagesTests.vietnamese()), clock,
        new TelegramNavigation(clock, new SecureRandom()), new ControlProofs());
  }

  private Update message(String text) {
    var message = new Message(); message.setMessageId(++event);
    message.setFrom(new User(77L, "owner", false)); message.setChat(new Chat(77L, "private"));
    message.setText(text); message.setDate(Math.toIntExact(clock.instant().getEpochSecond()));
    var update = new Update(); update.setUpdateId(event); update.setMessage(message); return update;
  }

  private Update callback(Message message, String data) {
    var query = new CallbackQuery(); query.setId("callback-" + ++event);
    query.setFrom(new User(77L, "owner", false)); query.setMessage(message); query.setData(data);
    var update = new Update(); update.setUpdateId(event); update.setCallbackQuery(query); return update;
  }

  private String button(String label) {
    return ((InlineKeyboardMarkup) sends.getLast().getReplyMarkup()).getKeyboard().stream()
        .flatMap(List::stream).filter(button -> button.getText().equals(label)).findFirst()
        .orElseThrow().getCallbackData();
  }

  private DeepSeekInterpreter queryInterpreter(ChatModel provider, RangeFilter filter,
      SimpleAsyncTaskExecutor executor) {
    String evidence = switch (filter) {
      case INCOME -> "khoản thu";
      case EXPENSE -> "khoản chi";
      case ALL -> "giao dịch";
    };
    String json = """
        {"intent":"LIST_RANGE","from":"2026-10-01","to":"2026-10-31",
         "filter":"%s","intentEvidence":"Cho tôi","filterEvidence":"%s"}
        """.formatted(filter.name(), evidence);
    when(provider.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
        new AssistantMessage(json), ChatGenerationMetadata.builder().finishReason("stop").build()))));
    return new DeepSeekInterpreter(new TransactionInterpreter(), provider,
        JsonMapper.builder().build(), executor);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INCOME", "EXPENSE", "ALL"})
  void conversationalQueryDispatchesExactServiceFilterAndSummaryWithoutMutation(String selected) {
    RangeFilter filter = RangeFilter.valueOf(selected);
    var provider = mock(ChatModel.class);
    try (var executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())) {
      restart(queryInterpreter(provider, filter, executor));
      String scope = switch (filter) {
        case INCOME -> "khoản thu";
        case EXPENSE -> "khoản chi";
        case ALL -> "giao dịch";
      };
      capture.handle(message("Cho tôi " + scope + " từ 2026-10-01 đến 2026-10-31."));
      assertThat(sends.getLast().getText()).contains("2026-10-01", "2026-10-31", "Asia/Ho_Chi_Minh");
      switch (filter) {
        case INCOME -> assertThat(sends.getLast().getText()).contains("25000000", "1 giao dịch", "#100")
            .doesNotContain("#23");
        case EXPENSE -> assertThat(sends.getLast().getText()).contains("920000", "23 giao dịch", "#23")
            .doesNotContain("#100");
        case ALL -> assertThat(sends.getLast().getText()).contains("25000000", "920000", "24080000");
      }
      var expected = DateRange.parse("2026-10-01", "2026-10-31");
      verify(store).totals("personal", expected, filter);
      verify(store).range("personal", expected, filter, Optional.empty());
      verify(provider, times(1)).call(any(Prompt.class));
      verifyNoInteractions(transactions);
    }
  }

  @Test
  void conversationalPageNavigatesAndSelectsCurrentRevisionWithExistingL2Controls() {
    var provider = mock(ChatModel.class);
    try (var executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())) {
      restart(queryInterpreter(provider, RangeFilter.EXPENSE, executor));
      capture.handle(message("Cho tôi các khoản chi từ 2026-10-01 đến 2026-10-31."));
      capture.handle(callback(delivered.getLast(), button("Trang sau")));
      assertThat(sends.getLast().getText()).contains("#13", "920000").doesNotContain("#23");
      capture.handle(callback(delivered.getLast(), button("Trang trước")));
      assertThat(sends.getLast().getText()).contains("#23", "920000");
      Entry original = rows.get(22);
      rows.set(22, new Entry(original.owner(), original.id(), 2,
          new CreateIntent(new TransactionDraft(TransactionType.EXPENSE, new Money(41000),
              Category.FOOD, new Note("mới")), original.intent().transactionDate()),
          original.createdAt(), original.messageAt(), false));
      capture.handle(callback(delivered.getLast(), button("Xem #23")));
      assertThat(sends.getLast().getText()).contains("Phiên bản 2", "41000", "mới");
      assertThat(button("Sửa")).startsWith("v:");
      assertThat(button("Xóa")).startsWith("v:");
      verify(provider, times(1)).call(any(Prompt.class));
      verifyNoInteractions(transactions);
    }
  }

  @Test
  void foreignQueryNeverCallsInterpreterAndMissingDatesDoNotReadOrWrite() {
    var provider = mock(ChatModel.class);
    try (var executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())) {
      restart(queryInterpreter(provider, RangeFilter.EXPENSE, executor));
      var foreign = message("Cho tôi các khoản chi từ 2026-10-01 đến 2026-10-31.");
      foreign.getMessage().getFrom().setId(88L);
      capture.handle(foreign);
      assertThat(sends).isEmpty();
      capture.handle(message("Cho tôi các khoản chi hôm qua"));
      assertThat(sends.getLast().getText()).contains("hai ngày cụ thể", "YYYY-MM-DD")
          .doesNotContain("Đã lưu", "Vui lòng gửi lại một giao dịch");
      verifyNoInteractions(provider, transactions, store);
    }
  }

  @Test
  void overviewAndTypedListAreDistinctAndShowFullTotals() {
    capture.handle(message("/overview 2026-10-01 2026-10-31"));
    assertThat(sends.getLast().getText()).contains("25000000", "920000", "24080000",
        "2026-10-01", "2026-10-31", "Asia/Ho_Chi_Minh").doesNotContain("#");
    assertThat(sends.getLast().getReplyMarkup()).isNull();
    capture.handle(message("/income 2026-10-01 2026-10-31"));
    assertThat(sends.getLast().getText()).contains("Thu nhập: 25000000", "1 giao dịch", "#100");
    capture.handle(message("/expenses 2026-10-01 2026-10-31"));
    assertThat(sends.getLast().getText()).contains("920000", "23 giao dịch", "#23").doesNotContain("#100");
    assertThat(button("Trang sau")).startsWith("r:");
  }

  @Test
  void nextPreviousAndSelectionUseCurrentRecordNotDisplayedSnapshot() {
    capture.handle(message("/expenses 2026-10-01 2026-10-31"));
    capture.handle(callback(delivered.getLast(), button("Trang sau")));
    assertThat(sends.getLast().getText()).contains("#13", "920000").doesNotContain("#23");
    capture.handle(callback(delivered.getLast(), button("Trang trước")));
    assertThat(sends.getLast().getText()).contains("#23");
    Entry original = rows.get(22);
    rows.set(22, new Entry(original.owner(), original.id(), 2,
        new CreateIntent(new TransactionDraft(TransactionType.EXPENSE, new Money(41000),
            Category.FOOD, new Note("mới")), original.intent().transactionDate()),
        original.createdAt(), original.messageAt(), false));
    capture.handle(callback(delivered.getLast(), button("Xem #23")));
    assertThat(sends.getLast().getText()).contains("Phiên bản 2", "41000", "mới");
    assertThat(button("Sửa")).startsWith("v:");
  }

  @Test
  void changedGenerationRefreshesSameTypeRangeAndFullSummary() {
    capture.handle(message("/expenses 2026-10-01 2026-10-31"));
    String next = button("Trang sau"); Message displayed = delivered.getLast();
    rows.remove(22); generation.incrementAndGet();
    capture.handle(callback(displayed, next));
    assertThat(sends.getLast().getText()).contains("22 giao dịch", "880000", "#22",
        "2026-10-01", "2026-10-31").doesNotContain("#100");
  }

  @Test
  void expiryAndRestartSelectionControlsRefreshSameRangeInsteadOfRecent() {
    capture.handle(message("/expenses 2026-10-01 2026-10-31"));
    String next = button("Trang sau"); Message old = delivered.getLast();
    clock.now = clock.now.plusSeconds(600);
    capture.handle(callback(old, next));
    assertThat(sends.getLast().getText()).contains("920000", "23 giao dịch", "#23", "2026-10-01");
    String selected = button("Xem #23"); Message second = delivered.getLast();
    restart();
    capture.handle(callback(second, selected));
    assertThat(sends.getLast().getText()).contains("23 giao dịch", "#23", "2026-10-31")
        .doesNotContain("Phiên bản");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/overview", "/overview 2026-10-20", "/income 2026-02-30 2026-03-01",
      "/expenses 2026-10-31 2026-10-01", "/transactions 1899-01-01 2026-01-01",
      "/overview 2026-10-01 2026-10-31 extra"})
  void invalidCommandsClarifyInsteadOfChoosingAnotherRange(String command) {
    capture.handle(message(command));
    assertThat(sends.getLast().getText()).startsWith("Khoảng ngày chưa hợp lệ.");
  }

  @Test
  void copiedForeignAndChangedKnownQueryControlsCannotNavigate() {
    capture.handle(message("/expenses 2026-10-01 2026-10-31"));
    String next = button("Trang sau"); Message original = delivered.getLast(); int count = sends.size();
    var copied = new Message(); copied.setMessageId(999); copied.setChat(new Chat(77L, "private"));
    capture.handle(callback(copied, next));
    var foreign = callback(original, next); foreign.getCallbackQuery().getFrom().setId(88L);
    capture.handle(foreign);
    String[] fields = next.split(":");
    capture.handle(callback(original, next.replace(fields[2], "i" + fields[2].substring(1))));
    assertThat(sends).hasSize(count);
    capture.handle(callback(original, "r:unknown:bad:n"));
    assertThat(sends.getLast().getText()).startsWith("Khoảng ngày chưa hợp lệ.");
  }

  @Test
  void callbacksStayBelow64BytesForMaximumIdAndDatesAndHistoryIsBounded() {
    var range = DateRange.parse("1900-01-01", "9999-12-31");
    var view = mapper.view(row(Long.MAX_VALUE, TransactionType.INCOME, 1));
    var totals = mapper.summary(range, RangeFilter.ALL,
        new Totals(BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO), 1);
    var page = new TransactionPage("range", List.of(view), 10, "1", Optional.of("next"),
        Optional.of(totals));
    var nav = new TelegramNavigation(clock, new SecureRandom());
    var selection = new RangeSelection(range, RangeFilter.ALL);
    Route route = Route.first(selection);
    for (int i = 0; i < 105; i++) {
      String token = nav.open(77, page, route); nav.bind(token, 100 + i);
      String data = TelegramNavigation.pageCallback(page, token, "n");
      route = nav.resolve(data, 77, 100 + i).orElseThrow();
    }
    assertThat(route.previous()).hasSize(100);
    String token = nav.open(77, page, route); nav.bind(token, 300);
    String data = TelegramNavigation.pageCallback(page, token, "s:" + Long.MAX_VALUE);
    assertThat(data.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
    assertThat(nav.resolve(data, 77, 300).orElseThrow().entryId())
        .contains(Long.toString(Long.MAX_VALUE));
    assertThatThrownBy(() -> nav.resolve(data, 77, 301)).isInstanceOf(SecurityException.class);
  }

  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-10-21T00:00:00Z");
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    @Override public Instant instant() { return now; }
  }
}
