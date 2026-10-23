package me.nghlong3004.longnest.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.LedgerException.Code;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.presentation.UiMessagesTests;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramFailure;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation.Route;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** SDK callbacks and real navigation state; all service/client effects are synthetic. */
class TelegramBrowsingTests {
  private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
  private final AtomicReference<Instant> now = new AtomicReference<>(NOW);
  private final Clock clock = mock(Clock.class);
  private final TelegramClient client = mock(TelegramClient.class);
  private final LedgerClient ledger = mock(LedgerClient.class);
  private final List<SendMessage> sends = new ArrayList<>();
  private final List<Message> delivered = new ArrayList<>();
  private TelegramNavigation navigation;
  private TelegramCapture capture;
  private int event = 10;

  @BeforeEach
  void setUp() throws Exception {
    when(clock.instant()).thenAnswer(call -> now.get());
    navigation = new TelegramNavigation(clock, new SecureRandom());
    capture = capture(navigation);
    when(client.execute(any(AnswerCallbackQuery.class))).thenReturn(true);
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      SendMessage request = call.getArgument(0);
      assertThat(request.getChatId()).isEqualTo("77");
      if (request.getReplyMarkup() != null) request.getReplyMarkup().validate();
      sends.add(request);
      var message = new Message();
      message.setMessageId(100 + sends.size());
      message.setChat(new Chat(77L, "private"));
      delivered.add(message);
      return message;
    });
    when(ledger.recent(any(), eq(Optional.empty()))).thenReturn(new LedgerResult.Ok<>(first()));
    when(ledger.recent(any(), eq(Optional.of("next-1"))))
        .thenReturn(new LedgerResult.Ok<>(page(List.of(view("32", "1")), Optional.empty())));
  }

  private TelegramCapture capture(TelegramNavigation state) {
    return new TelegramCapture(client, new TelegramSettings("123:" + "x".repeat(30), 77),
        new PersonalOwner("personal"), new TransactionInterpreter(), mock(TransactionMapper.class),
        ledger, new TelegramReplies(UiMessagesTests.vietnamese()), clock, state,
        new ControlProofs());
  }

  private static TransactionView view(String id, String revision) {
    return new TransactionView(id, revision, TransactionType.EXPENSE, "45000", "VND", Category.FOOD,
        "lunch", LocalDate.parse("2026-10-20"), NOW, Optional.of(NOW),
        TransactionView.State.ACTIVE);
  }

  private static TransactionPage page(List<TransactionView> entries, Optional<String> next) {
    return new TransactionPage("recent", entries, 10, "4", next);
  }

  private static TransactionPage first() {
    return page(IntStream.range(0, 10).mapToObj(i -> view(Integer.toString(42 - i), "1")).toList(),
        Optional.of("next-1"));
  }

  private Update command(String text) {
    var message = new Message();
    message.setMessageId(++event);
    message.setChat(new Chat(77L, "private"));
    message.setFrom(new User(77L, "owner", false));
    message.setText(text);
    message.setDate(Math.toIntExact(NOW.getEpochSecond()));
    var update = new Update();
    update.setUpdateId(event);
    update.setMessage(message);
    return update;
  }

  private Update callback(Message message, String data) {
    var query = new CallbackQuery();
    query.setId("query-" + ++event);
    query.setFrom(new User(77L, "owner", false));
    query.setMessage(message);
    query.setData(data);
    var update = new Update();
    update.setUpdateId(event);
    update.setCallbackQuery(query);
    return update;
  }

  private InlineKeyboardMarkup keyboard() {
    return (InlineKeyboardMarkup) sends.getLast().getReplyMarkup();
  }

  private String button(String label) {
    return keyboard().getKeyboard().stream().flatMap(List::stream)
        .filter(b -> b.getText().equals(label)).findFirst().orElseThrow().getCallbackData();
  }

  private List<String> labels() {
    return keyboard() == null ? List.of() : keyboard().getKeyboard().stream()
        .flatMap(List::stream).map(InlineKeyboardButton::getText).toList();
  }

  @AfterEach
  void readsNeverMutateOrRecoverPurchases() {
    verify(ledger, never()).create(any(), any());
    verify(ledger, never()).recover(any());
    verify(ledger, never()).pendingCreates(any(), any());
  }

  @Test
  void recentNextPreviousSelectFetchesCurrentRecord() throws Exception {
    assertThat(capture.handle(command("/recent"))).isTrue();
    assertThat(labels()).hasSize(11).contains("Xem #42", "Trang sau").doesNotContain("Trang trước");
    assertThat(capture.handle(callback(delivered.getLast(), button("Trang sau")))).isTrue();
    assertThat(labels()).containsExactly("Xem #32", "Trang trước");
    assertThat(capture.handle(callback(delivered.getLast(), button("Trang trước")))).isTrue();
    assertThat(labels()).contains("Xem #42", "Trang sau").doesNotContain("Trang trước");
    var current = new TransactionView("42", "9", TransactionType.EXPENSE, "40000", "VND",
        Category.FOOD, "corrected", LocalDate.parse("2026-10-19"), NOW, Optional.of(NOW),
        TransactionView.State.DELETED);
    when(ledger.get(any(), eq("42"))).thenReturn(new LedgerResult.Ok<>(current));
    var restored = new TransactionView("42", "9", TransactionType.EXPENSE, "40000", "VND",
        Category.FOOD, "corrected", LocalDate.parse("2026-10-19"), NOW, Optional.of(NOW),
        TransactionView.State.ACTIVE);
    String action = "a".repeat(32);
    when(ledger.openRestoreButton(any(), eq("42"), eq("9"))).thenReturn(new LedgerResult.Ok<>(
        new ConfirmationRequired(action, ConfirmationRequired.Kind.RESTORE, current,
            restored, "9", NOW.plusSeconds(600), null)));
    when(ledger.bindAction(any(), eq(action), anyLong())).thenReturn(new LedgerResult.Ok<>(true));
    var select = callback(delivered.getLast(), button("Xem #42"));
    assertThat(capture.handle(select)).isTrue();
    assertThat(sends.getLast().getText()).contains("Phiên bản 9", "Đã xóa", "40000", "2026-10-19");
    assertThat(labels()).containsExactly("Khôi phục");
    var context = ArgumentCaptor.forClass(HostContext.class);
    verify(ledger).get(context.capture(), eq("42"));
    assertThat(context.getValue().principal()).isEqualTo("personal");
    assertThat(context.getValue().operationId()).isEqualTo("tg:123:" + select.getUpdateId());
    assertThat(context.getValue().messageAt()).isNull();
    verify(client, times(3)).execute(any(AnswerCallbackQuery.class));
  }

  @Test
  void repeatedNextFromSameMessageDoesNotSkipAnotherPage() throws Exception {
    capture.handle(command("/recent"));
    var next = callback(delivered.getLast(), button("Trang sau"));
    capture.handle(next);
    capture.handle(next);
    verify(ledger, times(2)).recent(any(), eq(Optional.of("next-1")));
    assertThat(labels()).containsExactly("Xem #32", "Trang trước");
  }

  @Test
  void repeatedSelectionAlwaysRetrievesCurrentState() throws Exception {
    capture.handle(command("/recent"));
    var select = callback(delivered.getLast(), button("Xem #42"));
    when(ledger.get(any(), eq("42"))).thenReturn(new LedgerResult.Ok<>(view("42", "2")))
        .thenReturn(new LedgerResult.Ok<>(view("42", "3")));
    capture.handle(select);
    capture.handle(select);
    assertThat(sends.getLast().getText()).contains("Phiên bản 3");
    verify(ledger, times(2)).get(any(), eq("42"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"foreign-user", "bot", "group", "foreign-chat", "inline", "no-actor"})
  void unauthorizedCallbacksAreSilentAndCannotQuery(String kind) throws Exception {
    capture.handle(command("/recent"));
    var update = callback(delivered.getLast(), button("Xem #42"));
    var query = update.getCallbackQuery();
    if (kind.equals("foreign-user")) query.getFrom().setId(88L);
    if (kind.equals("bot")) query.getFrom().setIsBot(true);
    if (kind.equals("no-actor")) query.setFrom(null);
    if (kind.equals("group")) delivered.getLast().getChat().setType("group");
    if (kind.equals("foreign-chat")) delivered.getLast().getChat().setId(88L);
    if (kind.equals("inline")) query.setMessage(null);
    clearInvocations(ledger, client);
    assertThat(capture.handle(update)).isTrue();
    verifyNoInteractions(ledger, client);
  }

  @Test
  void copiedControlOnAnotherMessageCannotQuery() throws Exception {
    capture.handle(command("/recent"));
    var message = new Message();
    message.setMessageId(999);
    message.setChat(new Chat(77L, "private"));
    var copied = callback(message, button("Xem #42"));
    clearInvocations(ledger);
    assertThat(capture.handle(copied)).isTrue();
    verifyNoInteractions(ledger);
  }

  @ParameterizedTest
  @EnumSource(value = Code.class, names = {"INVALID_CURSOR", "STALE_CURSOR"})
  void staleOrInvalidNextRefreshesFirstAndDropsHistory(Code code) throws Exception {
    capture.handle(command("/recent"));
    var next = callback(delivered.getLast(), button("Trang sau"));
    when(ledger.recent(any(), eq(Optional.of("next-1"))))
        .thenReturn(new LedgerResult.Failure<>(code, Optional.empty(), false));
    capture.handle(next);
    verify(ledger, times(2)).recent(any(), eq(Optional.empty()));
    assertThat(labels()).contains("Xem #42").doesNotContain("Trang trước");
  }

  @Test
  void expiredAndRestartedControlsRefreshInsteadOfSelectingOldRecord() throws Exception {
    capture.handle(command("/recent"));
    var select = callback(delivered.getLast(), button("Xem #42"));
    now.set(NOW.plus(Duration.ofMinutes(10)));
    capture.handle(select);
    capture = capture(new TelegramNavigation(clock, new SecureRandom()));
    capture.handle(select);
    verify(ledger, times(3)).recent(any(), eq(Optional.empty()));
    verify(ledger, never()).get(any(), any());
    assertThat(labels()).doesNotContain("Trang trước");
  }

  @Test
  void tamperedSelectionCannotGetAnUndisplayedId() throws Exception {
    capture.handle(command("/recent"));
    var tampered = callback(delivered.getLast(), button("Xem #42").replace(":s:42", ":s:999"));
    capture.handle(tampered);
    verify(ledger, never()).get(any(), any());
    verify(ledger, times(2)).recent(any(), eq(Optional.empty()));
  }

  @Test
  void oldCallbackAnswerFailureDoesNotStrandReadOnlyHandling() throws Exception {
    capture.handle(command("/recent"));
    var next = callback(delivered.getLast(), button("Trang sau"));
    when(client.execute(any(AnswerCallbackQuery.class)))
        .thenThrow(new TelegramApiException("synthetic expired query"));
    assertThat(capture.handle(next)).isTrue();
    assertThat(labels()).contains("Xem #32");
  }

  @Test
  void emptyPageHasNoDeadButtonsAndDirectCursorPreviousReturnsFirst() throws Exception {
    when(ledger.recent(any(), eq(Optional.empty())))
        .thenReturn(new LedgerResult.Ok<>(page(List.of(), Optional.empty())));
    capture.handle(command("/recent"));
    assertThat(keyboard()).isNull();
    capture.handle(command("/recent next-1"));
    assertThat(labels()).contains("Trang trước").doesNotContain("Trang sau");
    capture.handle(callback(delivered.getLast(), button("Trang trước")));
    assertThat(sends.getLast().getText()).contains("Chưa có giao dịch");
    assertThat(keyboard()).isNull();
  }

  @Test
  void sendFailureDoesNotBindUncertainControls() throws Exception {
    var request = ArgumentCaptor.forClass(SendMessage.class);
    when(client.execute(any(SendMessage.class)))
        .thenThrow(new TelegramApiException("synthetic delivery uncertainty"));
    assertThatThrownBy(() -> capture.handle(command("/recent")))
        .isInstanceOf(TelegramFailure.class);
    verify(client).execute(request.capture());
    var data = ((InlineKeyboardMarkup) request.getValue().getReplyMarkup())
        .getKeyboard().getFirst().getFirst().getCallbackData();
    assertThatThrownBy(() -> navigation.resolve(data, 77, 999))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void navigationCapacityEvictsOldestAndOnlyStoresBoundedHistory() {
    String oldest = navigation.open(77, first(), Route.first());
    navigation.bind(oldest, 1);
    for (int i = 0; i < 100; i++) {
      String token = navigation.open(77, first(), Route.first());
      navigation.bind(token, i + 2);
    }
    assertThat(navigation.resolve("b:" + oldest + ":n", 77, 1)).isEmpty();
    Route route = Route.first();
    for (int i = 0; i < 150; i++) {
      String token = navigation.open(77, first(), route);
      navigation.bind(token, 200 + i);
      route = navigation.resolve("b:" + token + ":n", 77, 200 + i).orElseThrow();
      assertThat(route.previous()).hasSizeLessThanOrEqualTo(100);
    }
    for (int i = 0; i < 101; i++) {
      String token = navigation.open(77, first(), route);
      navigation.bind(token, 500 + i);
      route = navigation.resolve("b:" + token + ":p", 77, 500 + i).orElseThrow();
    }
    assertThat(route).isEqualTo(Route.first());
  }

  @Test
  void callbackDataFitsTelegramLimitAndUnknownControlsNeverBecomePurchases() throws Exception {
    capture.handle(command("/recent"));
    for (var row : keyboard().getKeyboard()) {
      for (var button : row) {
        assertThat(button.getCallbackData().getBytes(StandardCharsets.UTF_8).length)
            .isLessThanOrEqualTo(64);
      }
    }
    clearInvocations(ledger);
    capture.handle(callback(delivered.getLast(), "/delete 42"));
    capture.handle(callback(delivered.getLast(), "ăn trưa 45k"));
    verifyNoInteractions(ledger);
  }

  @Test
  void threePagesBacktrackToTheirOriginalCursors() throws Exception {
    when(ledger.recent(any(), eq(Optional.of("next-1"))))
        .thenReturn(new LedgerResult.Ok<>(page(List.of(view("32", "1")), Optional.of("next-2"))));
    when(ledger.recent(any(), eq(Optional.of("next-2"))))
        .thenReturn(new LedgerResult.Ok<>(page(List.of(view("22", "1")), Optional.empty())));
    capture.handle(command("/recent"));
    capture.handle(callback(delivered.getLast(), button("Trang sau")));
    capture.handle(callback(delivered.getLast(), button("Trang sau")));
    assertThat(labels()).containsExactly("Xem #22", "Trang trước");
    capture.handle(callback(delivered.getLast(), button("Trang trước")));
    assertThat(labels()).containsExactly("Xem #32", "Trang trước", "Trang sau");
    capture.handle(callback(delivered.getLast(), button("Trang trước")));
    assertThat(labels()).contains("Xem #42").doesNotContain("Trang trước");
    verify(ledger, times(2)).recent(any(), eq(Optional.of("next-1")));
    verify(ledger, times(2)).recent(any(), eq(Optional.empty()));
  }

  @Test
  void unavailableReadRetriesSameCallbackWithoutAdvancingOrCreating() throws Exception {
    capture.handle(command("/recent"));
    var next = callback(delivered.getLast(), button("Trang sau"));
    when(ledger.recent(any(), eq(Optional.of("next-1"))))
        .thenReturn(new LedgerResult.Failure<>(Code.NOT_SAVED, Optional.empty(), true))
        .thenReturn(new LedgerResult.Ok<>(page(List.of(view("32", "1")), Optional.empty())));
    assertThat(capture.handle(next)).isFalse();
    assertThat(sends.getLast().getText()).contains("NOT_SAVED").doesNotContain("Đã lưu\n");
    assertThat(capture.handle(next)).isTrue();
    assertThat(labels()).containsExactly("Xem #32", "Trang trước");
    var contexts = ArgumentCaptor.forClass(HostContext.class);
    verify(ledger, times(2)).recent(contexts.capture(), eq(Optional.of("next-1")));
    assertThat(contexts.getAllValues()).extracting(HostContext::operationId)
        .containsOnly("tg:123:" + next.getUpdateId());
  }
}
