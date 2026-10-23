package me.nghlong3004.longnest.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.ControlBinding;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.ControlProof;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlKind;
import me.nghlong3004.longnest.presentation.UiMessagesTests;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Synthetic SDK messages and service results; no Telegram request or database. */
class TelegramL2Tests {
  private static final Instant NOW = Instant.parse("2026-10-21T04:00:00Z");
  private static final String FIRST = "a".repeat(32);
  private static final String SECOND = "b".repeat(32);
  private static final String PROMPT_ONE = "c".repeat(32);
  private static final String PROMPT_TWO = "d".repeat(32);
  private final TelegramClient client = mock(TelegramClient.class);
  private final LedgerClient ledger = mock(LedgerClient.class);
  private final TransactionMapper mapper = mock(TransactionMapper.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final List<SendMessage> sends = new ArrayList<>();
  private final List<Message> delivered = new ArrayList<>();
  private TelegramCapture capture;
  private int event = 1;

  private static TransactionView view(String amount, String date) {
    return new TransactionView("42", "1", TransactionType.EXPENSE, amount, "VND",
        Category.FOOD, "ăn trưa", LocalDate.parse(date), NOW, Optional.of(NOW),
        TransactionView.State.ACTIVE);
  }

  @BeforeEach
  void setup() throws Exception {
    capture = new TelegramCapture(client, new TelegramSettings("123:" + "x".repeat(30), 77),
        new PersonalOwner("personal"), new TransactionInterpreter(), mapper,
        ledger, new TelegramReplies(UiMessagesTests.vietnamese()), clock,
        new TelegramNavigation(clock, new SecureRandom()), new ControlProofs());
    when(client.execute(any(AnswerCallbackQuery.class))).thenReturn(true);
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      SendMessage request = call.getArgument(0);
      assertThat(request.getChatId()).isEqualTo("77");
      sends.add(request);
      var sent = new Message();
      sent.setMessageId(100 + sends.size());
      sent.setChat(new Chat(77L, "private"));
      delivered.add(sent);
      return sent;
    });
  }

  private Update message(String text, Message replyTo) {
    var sent = new Message();
    sent.setMessageId(++event);
    sent.setFrom(new User(77L, "owner", false));
    sent.setChat(new Chat(77L, "private"));
    sent.setDate(Math.toIntExact(NOW.getEpochSecond()));
    sent.setText(text);
    sent.setReplyToMessage(replyTo);
    var update = new Update();
    update.setUpdateId(event);
    update.setMessage(sent);
    return update;
  }

  private Update callback(Message displayed, String data) {
    var query = new CallbackQuery();
    query.setId("callback-" + ++event);
    query.setFrom(new User(77L, "owner", false));
    query.setMessage(displayed);
    query.setData(data);
    var update = new Update();
    update.setUpdateId(event);
    update.setCallbackQuery(query);
    return update;
  }

  private String button(String text) {
    return ((InlineKeyboardMarkup) sends.getLast().getReplyMarkup()).getKeyboard().stream()
        .flatMap(List::stream).filter(button -> button.getText().equals(text))
        .findFirst().orElseThrow().getCallbackData();
  }

  @Test
  void currentViewTwoFieldPreviewsCarryAmountAndDateBeforeOneSave() throws Exception {
    var before = view("45000", "2026-10-20");
    var amountAfter = view("40000", "2026-10-20");
    var combined = view("40000", "2026-10-19");
    when(ledger.get(any(), eq("42"))).thenReturn(new LedgerResult.Ok<>(before));
    when(ledger.openFieldPrompt(any(), eq("42"), eq("1"), eq(EditField.AMOUNT), isNull()))
        .thenReturn(new LedgerResult.Ok<>(new FieldPrompt(PROMPT_ONE, EditField.AMOUNT,
            before, before, NOW.plusSeconds(600), null)));
    when(ledger.openFieldPrompt(any(), eq("42"), eq("1"), eq(EditField.DATE), eq(FIRST)))
        .thenReturn(new LedgerResult.Ok<>(new FieldPrompt(PROMPT_TWO, EditField.DATE,
            before, amountAfter, NOW.plusSeconds(600), null)));
    when(ledger.bindPrompt(any(), any(), anyLong())).thenReturn(new LedgerResult.Ok<>(true));
    when(ledger.bindAction(any(), any(), anyLong())).thenReturn(new LedgerResult.Ok<>(true));
    when(ledger.previewFieldReply(any(), eq(103L), eq("40k"))).thenReturn(new LedgerResult.Ok<>(
        new ConfirmationRequired(FIRST, ConfirmationRequired.Kind.UPDATE, before, amountAfter,
            "1", NOW.plusSeconds(600), null)));
    when(ledger.previewFieldReply(any(), eq(105L), eq("2026-10-19")))
        .thenReturn(new LedgerResult.Ok<>(new ConfirmationRequired(SECOND,
            ConfirmationRequired.Kind.UPDATE, before, combined, "1", NOW.plusSeconds(600), null)));
    when(ledger.controlBinding(any(), eq(FIRST))).thenReturn(new LedgerResult.Ok<>(
        new ControlBinding(FIRST, ControlKind.UPDATE, 42, 1, 104L)));
    when(ledger.controlBinding(any(), eq(SECOND))).thenReturn(new LedgerResult.Ok<>(
        new ControlBinding(SECOND, ControlKind.UPDATE, 42, 1, 106L)));
    when(ledger.update(any(), eq(SECOND), any(ControlProof.class))).thenReturn(new LedgerResult.Ok<>(
        new MutationReceipt(SECOND, "update", true, false, new TransactionView("42", "2",
            TransactionType.EXPENSE, "40000", "VND", Category.FOOD, "ăn trưa",
            LocalDate.parse("2026-10-19"), NOW, Optional.of(NOW), TransactionView.State.ACTIVE),
            "1")));

    assertThat(capture.handle(message("/show 42", null))).isTrue();
    assertThat(capture.handle(callback(delivered.getLast(), button("Sửa")))).isTrue();
    assertThat(capture.handle(callback(delivered.getLast(), button("Số tiền")))).isTrue();
    assertThat(capture.handle(message("40k", delivered.getLast()))).isTrue();
    assertThat(sends.getLast().getText()).contains("Trước:", "Sau:", "45000", "40000");
    assertThat(capture.handle(callback(delivered.getLast(), button("Ngày giao dịch")))).isTrue();
    assertThat(capture.handle(message("2026-10-19", delivered.getLast()))).isTrue();
    assertThat(sends.getLast().getText()).contains("45000", "40000", "2026-10-20",
        "2026-10-19");
    assertThat(capture.handle(callback(delivered.getLast(), button("Lưu thay đổi")))).isTrue();
    assertThat(sends.getLast().getText()).contains("Phiên bản 2", "40000", "2026-10-19");
    verify(ledger).openFieldPrompt(any(), eq("42"), eq("1"), eq(EditField.DATE), eq(FIRST));
    verify(ledger).update(any(), eq(SECOND), any(ControlProof.class));
    verify(ledger, never()).create(any(), any());
  }

  @Test
  void expiredReplyCannotFallThroughToOrdinaryPurchaseAfterRestart() throws Exception {
    var oldBotPrompt = new Message();
    oldBotPrompt.setMessageId(512);
    oldBotPrompt.setChat(new Chat(77L, "private"));
    when(ledger.previewFieldReply(any(), eq(512L), eq("ăn trưa 45k")))
        .thenReturn(new LedgerResult.Failure<>(LedgerException.Code.ACTION_EXPIRED,
            Optional.empty(), false));
    assertThat(capture.handle(message("ăn trưa 45k", oldBotPrompt))).isTrue();
    assertThat(sends.getLast().getText()).contains("không còn hợp lệ", "không được lưu");
    verify(ledger, never()).create(any(), any());
  }

  @Test
  void typeAndCategoryChoicesAreVietnameseAndCarryCurrentProposedState() {
    var before = view("45000", "2026-10-20");
    var proposed = view("40000", "2026-10-19");
    var replies = new TelegramReplies(UiMessagesTests.vietnamese());
    var type = new FieldPrompt(PROMPT_ONE, EditField.TYPE, before, proposed,
        NOW.plusSeconds(600), null);
    assertThat(replies.prompt(type)).contains("Loại hiện tại: Chi tiêu", "40000", "2026-10-19");
    assertThat(replies.promptKeyboard(type).getKeyboard()).extracting(row -> row.getFirst().getText())
        .contains("Thu nhập", "Chi tiêu");
    var category = new FieldPrompt(PROMPT_TWO, EditField.CATEGORY, before, proposed,
        NOW.plusSeconds(600), null);
    assertThat(replies.promptKeyboard(category).getKeyboard()).extracting(row -> row.getFirst().getText())
        .contains("Ăn uống", "Đi chợ", "Chưa phân loại")
        .doesNotContain("Lương");
  }

  @Test
  void deleteCommandOnlyPreviewsAndRemovedRestoreCommandCannotMintNewIntent() throws Exception {
    var before = view("45000", "2026-10-20");
    var deleted = new TransactionView("42", "2", TransactionType.EXPENSE, "45000", "VND",
        Category.FOOD, "ăn trưa", LocalDate.parse("2026-10-20"), NOW, Optional.of(NOW),
        TransactionView.State.DELETED);
    when(ledger.requestDeleteCurrent(any(), eq("42"))).thenReturn(new LedgerResult.Ok<>(
        new ConfirmationRequired(FIRST, ConfirmationRequired.Kind.DELETE, before,
            deleted, "1", NOW.plusSeconds(600), null)));
    when(ledger.bindAction(any(), eq(FIRST), anyLong())).thenReturn(new LedgerResult.Ok<>(true));
    assertThat(capture.handle(message("/delete 42", null))).isTrue();
    assertThat(sends.getLast().getText()).contains("Xác nhận xóa", "#42", "45000",
        "2026-10-20");
    verify(ledger, never()).confirmDelete(any(), any(), any());
    assertThat(capture.handle(message("/restore 42", null))).isTrue();
    verify(ledger).requestDeleteCurrent(any(), eq("42"));
    verify(ledger, never()).restore(any(), any(), any());
    verify(ledger, never()).confirmDelete(any(), any(), any());
  }

  @Test
  void unboundValidPurchaseStillUsesCapturePipeline() throws Exception {
    var before = view("45000", "2026-10-20");
    when(mapper.input(any())).thenReturn(new TransactionInput(TransactionType.EXPENSE,
        "45000", "VND", Optional.empty(), Optional.of(Category.FOOD), Optional.of("ăn trưa")));
    when(ledger.create(any(), any())).thenReturn(new LedgerResult.Ok<>(
        new MutationReceipt("tg:123:2", "create", true, false, before, "1")));
    assertThat(capture.handle(message("ăn trưa 45k", null))).isTrue();
    verify(ledger).create(any(), any());
  }

  @Test
  void copiedOrForeignEntryControlsCannotOpenDeleteReview() throws Exception {
    when(ledger.get(any(), eq("42"))).thenReturn(new LedgerResult.Ok<>(
        view("45000", "2026-10-20")));
    capture.handle(message("/show 42", null));
    String delete = button("Xóa");
    Message original = delivered.getLast();
    var copied = new Message();
    copied.setMessageId(999);
    copied.setChat(new Chat(77L, "private"));
    assertThat(capture.handle(callback(copied, delete))).isTrue();
    var foreign = callback(original, delete);
    foreign.getCallbackQuery().getFrom().setId(88L);
    assertThat(capture.handle(foreign)).isTrue();
    verify(ledger, never()).requestDelete(any(), any(), any());
  }

  @Test
  void noteClearButtonPreviewsEmptyNoteWithoutAnEmptyTelegramText() throws Exception {
    var before = view("45000", "2026-10-20");
    var cleared = new TransactionView("42", "1", TransactionType.EXPENSE, "45000", "VND",
        Category.FOOD, "", LocalDate.parse("2026-10-20"), NOW, Optional.of(NOW),
        TransactionView.State.ACTIVE);
    when(ledger.get(any(), eq("42"))).thenReturn(new LedgerResult.Ok<>(before));
    when(ledger.openFieldPrompt(any(), eq("42"), eq("1"), eq(EditField.NOTE), isNull()))
        .thenReturn(new LedgerResult.Ok<>(new FieldPrompt(PROMPT_ONE, EditField.NOTE,
            before, before, NOW.plusSeconds(600), null)));
    when(ledger.bindPrompt(any(), eq(PROMPT_ONE), anyLong())).thenReturn(new LedgerResult.Ok<>(true));
    when(ledger.controlBinding(any(), eq(PROMPT_ONE))).thenReturn(new LedgerResult.Ok<>(
        new ControlBinding(PROMPT_ONE, ControlKind.PROMPT, 42, 1, 103L)));
    when(ledger.previewFieldReply(any(), eq(103L), eq(""))).thenReturn(new LedgerResult.Ok<>(
        new ConfirmationRequired(FIRST, ConfirmationRequired.Kind.UPDATE, before, cleared,
            "1", NOW.plusSeconds(600), null)));
    when(ledger.bindAction(any(), eq(FIRST), anyLong())).thenReturn(new LedgerResult.Ok<>(true));
    capture.handle(message("/show 42", null));
    capture.handle(callback(delivered.getLast(), button("Sửa")));
    capture.handle(callback(delivered.getLast(), button("Ghi chú")));
    assertThat(button("Xóa ghi chú")).startsWith("p:");
    assertThat(capture.handle(callback(delivered.getLast(), button("Xóa ghi chú")))).isTrue();
    verify(ledger).previewFieldReply(any(), eq(103L), eq(""));
    assertThat(sends.getLast().getText()).contains("Sau:", "Ghi chú:");
    verify(ledger, never()).create(any(), any());
  }
}
