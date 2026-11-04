package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.longnest.integration.telegram.TelegramMessageSender;
import me.nghlong3004.longnest.integration.telegram.TelegramTransactionActions;
import me.nghlong3004.longnest.integration.telegram.TelegramTransactionKeyboard;
import me.nghlong3004.longnest.integration.telegram.TelegramTransactionQueryCodec;
import me.nghlong3004.longnest.integration.telegram.TelegramTransactionRegistry;
import me.nghlong3004.longnest.mapper.TransactionMapper;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.ChatResponse;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.ChatAction;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionField;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.presentation.TransactionActionPresentation;
import me.nghlong3004.longnest.presentation.TransactionMessageFormatter;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionMutationService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import me.nghlong3004.longnest.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@SpringBootTest(
    properties = {
      "spring.main.keep-alive=false",
      "longnest.telegram.enabled=false",
      "longnest.deepseek.enabled=false"
    })
@Import(TestcontainersConfiguration.class)
class TelegramTransactionMutationTests {
  @Autowired TransactionMutationService mutations;
  @Autowired TransactionQueryService queries;
  @Autowired TransactionService recording;
  @Autowired TransactionRepository transactions;
  @Autowired JdbcTemplate jdbc;
  @Autowired ChatService chatService;
  @Autowired DraftService draftService;
  @Autowired TransactionMapper mapper;
  TelegramClient client;
  TelegramTransactionActions actions;
  List<SendMessage> sent;
  List<EditMessageText> edited;
  Map<Integer, InlineKeyboardMarkup> keyboards;
  int sequence;
  long owner;
  ChatMessage source;
  UUID id;
  int card;
  TransactionPage page;
  Clock clock;

  @BeforeEach
  void setup() throws Exception {
    client = mock(TelegramClient.class);
    sent = new ArrayList<>();
    edited = new ArrayList<>();
    keyboards = new HashMap<>();
    sequence = 100;
    when(client.execute(any(SendMessage.class)))
        .thenAnswer(
            call -> {
              SendMessage request = call.getArgument(0);
              sent.add(request);
              sequence++;
              keyboards.put(sequence, (InlineKeyboardMarkup) request.getReplyMarkup());
              return message(sequence, owner);
            });
    when(client.execute(any(EditMessageText.class)))
        .thenAnswer(
            call -> {
              EditMessageText request = call.getArgument(0);
              edited.add(request);
              keyboards.put(request.getMessageId(), request.getReplyMarkup());
              return null;
            });
    clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-11-02T00:00:00Z"));
    actions =
        new TelegramTransactionActions(
            new TelegramMessageSender(client),
            mutations,
            queries,
            new TransactionMessageFormatter(),
            new TelegramTransactionQueryCodec(),
            clock,
            mapper,
            new TransactionActionPresentation(new TransactionMessageFormatter()),
            new TelegramTransactionRegistry(clock),
            new TelegramTransactionKeyboard(
                new TransactionActionPresentation(new TransactionMessageFormatter())));
    owner = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1000000000) + 1000;
    source =
        new ChatMessage(
            Channel.TELEGRAM,
            Long.toString(owner),
            Long.toString(owner),
            "1",
            Instant.now(),
            "synthetic");
    id =
        recording
            .record(
                new TransactionData(
                    TransactionType.EXPENSE,
                    new BigDecimal("45000"),
                    TransactionCategory.FOOD,
                    "Lunch",
                    "Cafe",
                    LocalDate.parse("2026-11-02"),
                    TimeOfDay.NOON),
                source)
            .getId();
    page =
        queries.view(
            source,
            new TransactionQuery(
                TransactionType.EXPENSE,
                QueryPeriod.CUSTOM,
                LocalDate.parse("2026-11-02"),
                LocalDate.parse("2026-11-02")),
            null);
    actions.sendPage(Long.toString(owner), 1, new ChatReply("Summary", null, page));
    card = sequence;
  }

  @Test
  void editUsesPreviewSaveAndConsumesConfirmationWithoutCreationDraftWrites() throws Exception {
    var draftsBefore = jdbc.queryForList("select * from transaction_drafts order by id");
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    assertThat(edited.getLast().getText())
        .contains("Chọn phần cần sửa giúp em")
        .doesNotContain("Bạn", "bạn");
    click(prompt, "Số tiền");
    assertThat(edited.getLast().getText()).contains("Gửi em số tiền").doesNotContain("Bạn", "bạn");
    assertThat(actions.handleText(text("123.45"))).isTrue();
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
    assertThat(edited.getLast().getText()).contains("Trước", "Sau", "45.000", "123,45");
    var confirm = callback(prompt, button(prompt, "Lưu"), owner);
    actions.handleCallback(confirm, (Message) confirm.getMessage());
    actions.handleCallback(confirm, (Message) confirm.getMessage());
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("123.45");
    assertThat(transactions.findById(id).orElseThrow().getVersion()).isEqualTo(1);
    assertThat(jdbc.queryForList("select * from transaction_drafts order by id"))
        .isEqualTo(draftsBefore);
    assertThat(sent.getLast().getText()).contains("123,45");
  }

  @Test
  void cancelInputAndPreviewAreReadOnlyAndDoNotCancelCreationDraft() throws Exception {
    var pending =
        recording.record(
            new TransactionData(
                TransactionType.EXPENSE,
                new BigDecimal("99"),
                null,
                "another",
                null,
                LocalDate.parse("2026-11-02"),
                null),
            new ChatMessage(
                source.channel(),
                source.externalUserId(),
                source.chatId(),
                "other",
                source.sentAt(),
                "other"));
    jdbc.update(
        "insert into transaction_drafts(user_id, channel, chat_id, message_id, original_text)"
            + " select user_id, 'TELEGRAM', chat_id, 'pending', 'synthetic pending draft' from"
            + " transactions where id = ?",
        pending.getId());
    var draftsBefore = jdbc.queryForList("select * from transaction_drafts order by id");
    var before = jdbc.queryForMap("select * from transactions where id = ?", id);
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    actions.handleText(text("forty five"));
    assertThat(edited.getLast().getText()).doesNotContain("Sau");
    assertThat(actions.handleText(text("/cancel"))).isTrue();
    assertThat(jdbc.queryForMap("select * from transactions where id = ?", id)).isEqualTo(before);
    click(card, "⋯");
    prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Ngày");
    actions.handleText(text("2026-10-01"));
    click(prompt, "Hủy");
    assertThat(jdbc.queryForMap("select * from transactions where id = ?", id)).isEqualTo(before);
    assertThat(actions.handleText(text("ordinary"))).isFalse();
    assertThat(jdbc.queryForList("select * from transaction_drafts order by id"))
        .isEqualTo(draftsBefore);
  }

  @Test
  void deleteConfirmsSpecificIdAmountDateAndSurvivesNativeRemovalFailure() throws Exception {
    actions.sendPage(Long.toString(owner), 2, new ChatReply("Duplicate", null, page));
    int duplicate = sequence;
    when(client.execute(any(DeleteMessage.class)))
        .thenThrow(new TelegramApiException("synthetic old message"));
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Xóa");
    assertThat(edited.getLast().getText())
        .contains(id.toString(), "45.000", "02/11/2026", "vĩnh viễn", "Không thể hoàn tác", "em")
        .doesNotContain("Telegram", "Thanos", "hiệu ứng", "nỗ lực riêng");
    var confirm = callback(prompt, button(prompt, "Xóa vĩnh viễn"), owner);
    actions.handleCallback(confirm, (Message) confirm.getMessage());
    actions.handleCallback(confirm, (Message) confirm.getMessage());
    assertThat(transactions.existsById(id)).isFalse();
    assertThat(edited.getLast().getText())
        .contains("Em đã xóa vĩnh viễn", "chưa cập nhật hết tin hiển thị")
        .doesNotContain("Telegram", "Thanos", "hiệu ứng", "nỗ lực riêng");
    for (int visible : List.of(card, duplicate)) {
      assertThat(edited.stream().filter(edit -> edit.getMessageId() == visible).toList())
          .anySatisfy(
              edit -> {
                assertThat(edit.getText()).contains("đã xóa");
                assertThat(edit.getReplyMarkup().getKeyboard()).isEmpty();
              });
    }
    assertThat(sent.stream().map(SendMessage::getText))
        .anyMatch(value -> value.contains("Em chưa thấy khoản chi"));
    assertThat(edited.getLast().getText()).doesNotContain("thất bại");
  }

  @Test
  void rejectsCopiedTamperedWrongOwnerWrongChatAndStaleButtons() throws Exception {
    var token = button(card, "⋯");
    for (var bad :
        List.of(
            callback(card + 1, token, owner),
            callback(card, token, owner + 1),
            callback(card, token + "bad", owner))) {
      actions.handleCallback(bad, (Message) bad.getMessage());
    }
    assertThat(sent).hasSize(2);
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Xóa");
    var old = callback(prompt, button(prompt, "Xóa vĩnh viễn"), owner);
    var revision = mutations.load(source, id);
    mutations.save(
        source,
        id,
        revision.version(),
        mutations.preview(revision, TransactionField.AMOUNT, "999"));
    actions.handleCallback(old, (Message) old.getMessage());
    assertThat(transactions.existsById(id)).isTrue();
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("999");
  }

  @Test
  void databaseSuccessRemainsSuccessWhenAllTelegramUpdatesFail() throws Exception {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Xóa");
    var confirm = callback(prompt, button(prompt, "Xóa vĩnh viễn"), owner);
    when(client.execute(any(DeleteMessage.class))).thenThrow(new TelegramApiException("offline"));
    when(client.execute(any(EditMessageText.class))).thenThrow(new TelegramApiException("offline"));
    when(client.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("offline"));
    assertThatCode(() -> actions.handleCallback(confirm, (Message) confirm.getMessage()))
        .doesNotThrowAnyException();
    assertThat(transactions.existsById(id)).isFalse();
    actions.handleCallback(confirm, (Message) confirm.getMessage());
    assertThat(transactions.existsById(id)).isFalse();
  }

  @Test
  void expiredInputIsConsumedWithoutCreationOrMutation() {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    when(clock.instant()).thenReturn(Instant.parse("2026-11-02T00:16:00Z"));
    assertThat(actions.handleText(text("50k"))).isTrue();
    assertThat(edited.getLast().getText()).contains("hết hạn");
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
  }

  @Test
  void dateEditInvalidatesAllOldPagesAndRefreshesMembershipAndTotals() throws Exception {
    for (int i = 0; i < 6; i++)
      recording.record(
          new TransactionData(
              TransactionType.EXPENSE,
              new BigDecimal("1000"),
              null,
              "Extra",
              null,
              LocalDate.parse("2026-11-02"),
              null),
          new ChatMessage(
              source.channel(),
              source.externalUserId(),
              source.chatId(),
              "extra-" + i,
              source.sentAt(),
              "x"));
    var query =
        new TransactionQuery(
            TransactionType.EXPENSE,
            QueryPeriod.CUSTOM,
            LocalDate.parse("2026-11-02"),
            LocalDate.parse("2026-11-02"));
    var first = queries.view(source, query, null);
    var codec = new TelegramTransactionQueryCodec();
    var oldNext = codec.encode(first, 1);
    actions.sendPage(source.chatId(), 1, new ChatReply("first", null, first));
    int oldNextAnchor = sequence;
    var second = queries.view(source, query, first.nextCursor());
    actions.sendPage(source.chatId(), 1, new ChatReply("second", null, second));
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Ngày");
    actions.handleText(text("2026-10-01"));
    click(prompt, "Lưu");
    assertThat(queries.view(source, query, null).totals().count()).isEqualTo(6);
    assertThat(sent.stream().map(SendMessage::getText))
        .anyMatch(value -> value.contains("6.000đ") && value.contains("6 khoản"));
    assertThat(edited.stream().filter(edit -> edit.getMessageId() == card).toList())
        .anyMatch(
            edit ->
                edit.getText().contains("không còn dùng được")
                    && edit.getReplyMarkup().getKeyboard().isEmpty());
    assertThat(actions.isRevokedPage(source.chatId(), oldNext, oldNextAnchor)).isTrue();
  }

  @Test
  void previewShowsTheWholeSuppliedTextInsteadOfCardTruncation() {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Nội dung");
    var description = "a".repeat(480) + "visible-tail";
    actions.handleText(text(description));
    assertThat(edited.getLast().getText()).contains(description);
  }

  @Test
  void runtimeTransportFailureAfterCommitDoesNotReportFinancialFailure() throws Exception {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Xóa");
    var confirm = callback(prompt, button(prompt, "Xóa vĩnh viễn"), owner);
    when(client.execute(any(EditMessageText.class)))
        .thenThrow(new IllegalStateException("client closed"));
    assertThatCode(() -> actions.handleCallback(confirm, (Message) confirm.getMessage()))
        .doesNotThrowAnyException();
    assertThat(transactions.existsById(id)).isFalse();
    var answer = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
    verify(client, atLeastOnce()).execute(answer.capture());
    assertThat(answer.getAllValues().getLast().getText()).isNull();
  }

  @Test
  void deletingAfterAnEditAlsoRemovesOrMarksEveryOlderDuplicateCard() throws Exception {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    actions.handleText(text("50k"));
    click(prompt, "Lưu");
    int freshCard = sequence;
    when(client.execute(any(DeleteMessage.class))).thenThrow(new TelegramApiException("old card"));
    click(freshCard, "⋯");
    prompt = sequence;
    click(prompt, "Xóa");
    click(prompt, "Xóa vĩnh viễn");
    assertThat(transactions.existsById(id)).isFalse();
    assertThat(
            edited.stream()
                .filter(edit -> edit.getMessageId() == card)
                .toList()
                .getLast()
                .getText())
        .contains("đã xóa");
  }

  @Test
  void cancelCommandDuringMenuAndFieldSelectionNeverFallsThroughToCreation() {
    for (boolean selectFieldMenu : List.of(false, true)) {
      click(card, "⋯");
      int prompt = sequence;
      if (selectFieldMenu) click(prompt, "Sửa");
      assertThat(actions.handleText(text("/cancel"))).isTrue();
      assertThat(transactions.existsById(id)).isTrue();
      assertThat(actions.handleText(text("ordinary"))).isFalse();
    }
  }

  @Test
  void consumedEditTextCannotBecomeACreationOnReplayAfterCancelOrRestart() {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    var value = text("50k");
    actions.handleText(value);
    click(prompt, "Hủy");
    var replay =
        new ChatMessage(
            Channel.TELEGRAM,
            source.externalUserId(),
            source.chatId(),
            value.getMessageId().toString(),
            source.sentAt(),
            value.getText());
    assertThatThrownBy(
            () ->
                recording.record(
                    new TransactionData(
                        TransactionType.EXPENSE,
                        new BigDecimal("50000"),
                        null,
                        null,
                        null,
                        LocalDate.parse("2026-11-02"),
                        null),
                    replay))
        .isInstanceOf(RuntimeException.class);
    assertThat(chatService.reply(replay).text()).contains("thao tác");
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
    click(card, "⋯");
    var cancel = text("/cancel");
    actions.handleText(cancel);
    var cancelReplay =
        new ChatMessage(
            Channel.TELEGRAM,
            source.externalUserId(),
            source.chatId(),
            cancel.getMessageId().toString(),
            source.sentAt(),
            cancel.getText());
    assertThat(chatService.reply(cancelReplay).text()).contains("thao tác");
  }

  @Test
  void laterEditsNeverOverwriteDeletedCardFallbackWithAnOrdinaryStaleCard() throws Exception {
    recording.record(
        new TransactionData(
            TransactionType.EXPENSE,
            new BigDecimal("1000"),
            null,
            "Other",
            null,
            LocalDate.parse("2026-11-02"),
            null),
        new ChatMessage(
            source.channel(),
            source.externalUserId(),
            source.chatId(),
            "other",
            source.sentAt(),
            "other"));
    when(client.execute(any(DeleteMessage.class))).thenThrow(new TelegramApiException("old card"));
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Xóa");
    click(prompt, "Xóa vĩnh viễn");
    int otherCard = sequence;
    click(otherCard, "⋯");
    prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    actions.handleText(text("2000"));
    click(prompt, "Lưu");
    assertThat(
            edited.stream()
                .filter(edit -> edit.getMessageId() == card)
                .toList()
                .getLast()
                .getText())
        .contains("đã xóa");
  }

  @Test
  void aCreationInterpretationAlreadyInFlightCannotApplyConsumedEditInputToAPendingDraft() {
    jdbc.update(
        "insert into transaction_drafts(user_id, channel, chat_id, message_id, original_text)"
            + " select user_id, 'TELEGRAM', chat_id, 'pending-race', 'pending' from transactions"
            + " where id = ?",
        id);
    var value = text("50k");
    var incoming =
        new ChatMessage(
            source.channel(),
            source.externalUserId(),
            source.chatId(),
            value.getMessageId().toString(),
            source.sentAt(),
            value.getText());
    var expected = draftService.findContext(incoming).orElseThrow();
    var before = jdbc.queryForList("select * from transaction_drafts where id = ?", expected.id());
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    actions.handleText(value);
    var proposal =
        new ChatResponse(
            ChatAction.READY,
            new TransactionData(
                TransactionType.EXPENSE,
                new BigDecimal("50000"),
                null,
                null,
                null,
                LocalDate.parse("2026-11-02"),
                null),
            "ready");
    assertThatThrownBy(() -> draftService.apply(proposal, incoming, expected))
        .isInstanceOf(RuntimeException.class);
    assertThat(jdbc.queryForList("select * from transaction_drafts where id = ?", expected.id()))
        .isEqualTo(before);
  }

  @ParameterizedTest
  @CsvSource({
    "description,Nội dung,260",
    "location,Địa điểm,130",
    "description,Nội dung,2600",
    "location,Địa điểm,2600"
  })
  void selectedLegacyTextIsCompleteBeforeConfirmationAndCancelLeavesLedgerUnchanged(
      String field, String label, int emojiCount) {
    var legacy = "LEGACY-START:" + "😀".repeat(emojiCount) + ":LEGACY-END";
    jdbc.update("update transactions set " + field + " = ? where id = ?", legacy, id);
    var before = jdbc.queryForMap("select * from transactions where id = ?", id);
    var draftsBefore = jdbc.queryForList("select * from transaction_drafts order by id");
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, label);
    var oldCancel = button(prompt, "Hủy");
    int editsBefore = edited.size();
    int sendsBefore = sent.size();
    var replacement = "Giá trị mới 😀".repeat(8);
    actions.handleText(text(replacement));
    var chunks = previewChunks(editsBefore, sendsBefore);
    var complete = String.join("", chunks);
    assertThat(complete)
        .contains(legacy, replacement, "Trước", "Sau", "Các trường khác giữ nguyên");
    assertThat(chunks)
        .allSatisfy(
            chunk -> {
              assertThat(chunk.length()).isLessThanOrEqualTo(4096);
              assertThat(Character.isHighSurrogate(chunk.charAt(chunk.length() - 1))).isFalse();
              assertThat(Character.isLowSurrogate(chunk.charAt(0))).isFalse();
            });
    int finalAnchor = prompt;
    if (legacy.length() > 4096) {
      var delivered = sent.subList(sendsBefore, sent.size());
      assertThat(delivered.size()).isGreaterThan(1);
      for (int index = 0; index < delivered.size() - 1; index++) {
        assertThat(delivered.get(index).getReplyMarkup()).isNull();
      }
      finalAnchor = sequence;
      assertThat(finalAnchor).isNotEqualTo(prompt);
      var saveOnWrongAnchor = callback(prompt, button(finalAnchor, "Lưu"), owner);
      actions.handleCallback(saveOnWrongAnchor, (Message) saveOnWrongAnchor.getMessage());
    }
    var obsolete = callback(prompt, oldCancel, owner);
    actions.handleCallback(obsolete, (Message) obsolete.getMessage());
    assertThat(jdbc.queryForMap("select * from transactions where id = ?", id)).isEqualTo(before);
    assertThat(button(finalAnchor, "Lưu")).isNotBlank();
    click(finalAnchor, "Hủy");
    assertThat(jdbc.queryForMap("select * from transactions where id = ?", id)).isEqualTo(before);
    assertThat(jdbc.queryForList("select * from transaction_drafts order by id"))
        .isEqualTo(draftsBefore);
    assertThat(actions.handleText(text("ordinary"))).isFalse();
  }

  @Test
  void longPreviewSaveIsBoundToTheFinalChunkAndKeepsOtherFields() {
    var legacy = "OLD-LOCATION:" + "😀".repeat(2600) + ":END";
    jdbc.update("update transactions set location = ? where id = ?", legacy, id);
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Địa điểm");
    int editsBefore = edited.size();
    int sendsBefore = sent.size();
    actions.handleText(text("New location 😀"));
    assertThat(String.join("", previewChunks(editsBefore, sendsBefore)))
        .contains(legacy, "New location 😀");
    int finalAnchor = sequence;
    assertThat(finalAnchor).isNotEqualTo(prompt);
    assertThat(transactions.findById(id).orElseThrow().getLocation()).isEqualTo(legacy);
    var save = callback(finalAnchor, button(finalAnchor, "Lưu"), owner);
    actions.handleCallback(save, (Message) save.getMessage());
    actions.handleCallback(save, (Message) save.getMessage());
    var row = transactions.findById(id).orElseThrow();
    assertThat(row.getLocation()).isEqualTo("New location 😀");
    assertThat(row.getDescription()).isEqualTo("Lunch");
    assertThat(row.getAmount()).isEqualByComparingTo("45000");
    assertThat(row.getVersion()).isEqualTo(1);
  }

  @Test
  void amountShorthandPreviewStaysCompactAndDoesNotDisplayUnchangedLegacyText() {
    var legacy = "UNCHANGED-LEGACY-DESCRIPTION".repeat(300);
    jdbc.update("update transactions set description = ? where id = ?", legacy, id);
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    actions.handleText(text("50k"));
    assertThat(edited.getLast().getText())
        .contains("Trước", "45.000đ", "Sau", "50.000đ", "Các trường khác giữ nguyên")
        .doesNotContain("UNCHANGED-LEGACY-DESCRIPTION")
        .hasSizeLessThan(1000);
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
    click(prompt, "Lưu");
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("50000");
    assertThat(transactions.findById(id).orElseThrow().getDescription()).isEqualTo(legacy);
  }

  @Test
  void typePreviewIncludesTheDerivedCategoryErasureBeforeSave() {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Loại");
    actions.handleText(text("INCOME"));
    assertThat(edited.getLast().getText()).contains("EXPENSE", "INCOME", "Danh mục", "FOOD", "—");
    assertThat(transactions.findById(id).orElseThrow().getType())
        .isEqualTo(TransactionType.EXPENSE);
    click(prompt, "Hủy");
    assertThat(transactions.findById(id).orElseThrow().getCategory())
        .isEqualTo(TransactionCategory.FOOD);
  }

  @Test
  void failedFinalChunkNeverEnablesSaveAndCancelRemainsSafe() throws Exception {
    jdbc.update("update transactions set description = ? where id = ?", "😀".repeat(2600), id);
    var before = jdbc.queryForMap("select * from transactions where id = ?", id);
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Nội dung");
    var calls = new AtomicInteger();
    var failed = new AtomicReference<SendMessage>();
    when(client.execute(any(SendMessage.class)))
        .thenAnswer(
            call -> {
              SendMessage request = call.getArgument(0);
              if (calls.incrementAndGet() == 2) {
                failed.set(request);
                throw new TelegramApiException("synthetic final chunk failure");
              }
              sent.add(request);
              sequence++;
              keyboards.put(sequence, (InlineKeyboardMarkup) request.getReplyMarkup());
              return message(sequence, owner);
            });
    actions.handleText(text("Replacement"));
    assertThat(calls.get()).isEqualTo(2);
    assertThat(
            keyboards.get(prompt).getKeyboard().stream()
                .flatMap(Collection::stream)
                .map(button -> button.getText())
                .toList())
        .containsExactly("Hủy");
    var token =
        ((InlineKeyboardMarkup) failed.get().getReplyMarkup())
            .getKeyboard()
            .getFirst()
            .getFirst()
            .getCallbackData();
    var undeliveredSave = callback(prompt, token, owner);
    actions.handleCallback(undeliveredSave, (Message) undeliveredSave.getMessage());
    assertThat(jdbc.queryForMap("select * from transactions where id = ?", id)).isEqualTo(before);
    assertThat(actions.handleText(text("/cancel"))).isTrue();
    assertThat(jdbc.queryForMap("select * from transactions where id = ?", id)).isEqualTo(before);
    assertThat(actions.handleText(text("ordinary"))).isFalse();
  }

  @ParameterizedTest
  @CsvSource({"Sửa,false", "Xóa,false", "Số tiền,true"})
  void failedMenuOrFieldDeliveryCanRetryTheStillVisibleButton(String label, boolean field)
      throws Exception {
    click(card, "⋯");
    int prompt = sequence;
    if (field) click(prompt, "Sửa");
    var request = callback(prompt, button(prompt, label), owner);
    var attempts = new AtomicInteger();
    when(client.execute(any(EditMessageText.class)))
        .thenAnswer(
            call -> {
              if (attempts.getAndIncrement() == 0)
                throw new TelegramApiException("synthetic refusal");
              EditMessageText update = call.getArgument(0);
              edited.add(update);
              keyboards.put(update.getMessageId(), update.getReplyMarkup());
              return null;
            });
    actions.handleCallback(request, (Message) request.getMessage());
    actions.handleCallback(request, (Message) request.getMessage());
    assertThat(attempts.get()).isEqualTo(2);
    assertThat(edited.getLast().getText())
        .contains(field ? "số tiền dương" : label.equals("Xóa") ? "xóa vĩnh viễn" : "phần cần sửa");
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
  }

  @Test
  void invalidReplacementInputRetiresThePreviouslyDisplayedSave() {
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    actions.handleText(text("50k"));
    var obsoleteSave = callback(prompt, button(prompt, "Lưu"), owner);
    actions.handleText(text("invalid replacement"));
    actions.handleCallback(obsoleteSave, (Message) obsoleteSave.getMessage());
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
    assertThat(actions.handleText(text("60k"))).isTrue();
    click(prompt, "Lưu");
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("60000");
  }

  @Test
  void receiptFailureWithFailedErrorDeliveryStillRetiresPreviousSave() throws Exception {
    var guarded = mock(TransactionMutationService.class, delegatesTo(mutations));
    var formatter = new TransactionMessageFormatter();
    var presentation = new TransactionActionPresentation(formatter);
    actions =
        new TelegramTransactionActions(
            new TelegramMessageSender(client),
            guarded,
            queries,
            formatter,
            new TelegramTransactionQueryCodec(),
            clock,
            mapper,
            presentation,
            new TelegramTransactionRegistry(clock),
            new TelegramTransactionKeyboard(presentation));
    actions.sendPage(source.chatId(), 1, new ChatReply("Summary", null, page));
    card = sequence;
    click(card, "⋯");
    int prompt = sequence;
    click(prompt, "Sửa");
    click(prompt, "Số tiền");
    actions.handleText(text("50k"));
    var obsoleteSave = callback(prompt, button(prompt, "Lưu"), owner);
    doThrow(new IllegalStateException("synthetic receipt failure"))
        .when(guarded)
        .consumeInput(any(ChatMessage.class));
    when(client.execute(any(EditMessageText.class)))
        .thenThrow(new TelegramApiException("synthetic delivery refusal"));
    assertThat(actions.handleText(text("60k"))).isTrue();
    actions.handleCallback(obsoleteSave, (Message) obsoleteSave.getMessage());
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
    doAnswer(call -> mutations.consumeInput(call.getArgument(0)))
        .when(guarded)
        .consumeInput(any(ChatMessage.class));
    doAnswer(
            call -> {
              EditMessageText update = call.getArgument(0);
              edited.add(update);
              keyboards.put(update.getMessageId(), update.getReplyMarkup());
              return null;
            })
        .when(client)
        .execute(any(EditMessageText.class));
    assertThat(actions.handleText(text("70k"))).isTrue();
    click(prompt, "Lưu");
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("70000");
  }

  List<String> previewChunks(int editsBefore, int sendsBefore) {
    if (sent.size() > sendsBefore) {
      return sent.subList(sendsBefore, sent.size()).stream().map(SendMessage::getText).toList();
    }
    return edited.subList(editsBefore, edited.size()).stream()
        .map(EditMessageText::getText)
        .toList();
  }

  String button(int anchor, String label) {
    return keyboards.get(anchor).getKeyboard().stream()
        .flatMap(Collection::stream)
        .filter(button -> button.getText().equals(label))
        .findFirst()
        .orElseThrow()
        .getCallbackData();
  }

  void click(int anchor, String label) {
    var callback = callback(anchor, button(anchor, label), owner);
    actions.handleCallback(callback, (Message) callback.getMessage());
  }

  Message text(String value) {
    var result = message(10000 + sequence++, owner);
    result.setText(value);
    result.setFrom(new User(owner, "Synthetic", false));
    return result;
  }

  static Message message(int id, long chat) {
    return Message.builder()
        .messageId(id)
        .chat(new Chat(chat, "private"))
        .date(1793548800)
        .from(new User(999L, "Bot", true))
        .build();
  }

  static CallbackQuery callback(int anchor, String token, long owner) {
    var result = new CallbackQuery();
    result.setId(UUID.randomUUID().toString());
    result.setData(token);
    result.setFrom(new User(owner, "Synthetic", false));
    result.setMessage(message(anchor, owner));
    return result;
  }
}
