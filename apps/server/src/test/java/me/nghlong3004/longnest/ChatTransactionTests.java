package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.ChatAction;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.repository.DraftMessageLinkRepository;
import me.nghlong3004.longnest.repository.TransactionDraftRepository;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.repository.UserRepository;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    properties = {
      "spring.main.keep-alive=false",
      "longnest.telegram.enabled=false",
      "longnest.deepseek.enabled=false",
      "longnest.ledger.time-zone=Asia/Ho_Chi_Minh"
    })
@Import({TestcontainersConfiguration.class, ChatTransactionTests.ModelConfiguration.class})
class ChatTransactionTests {

  private static final String EXPENSE =
      """
      {"action":"READY","transaction":{"type":"EXPENSE","amount":45000,
      "category":"FOOD","description":"Ăn trưa","location":"Quán cơm","date":null,
      "timeOfDay":"NOON"},"reply":"Đã lưu 9 triệu theo lời model."}
      """;

  @Autowired private ChatService chatService;
  @Autowired private DraftService draftService;
  @Autowired private TransactionService transactionService;
  @Autowired private TransactionRepository transactions;
  @Autowired private TransactionDraftRepository drafts;
  @Autowired private DraftMessageLinkRepository draftLinks;
  @Autowired private UserIdentityRepository identities;
  @Autowired private UserRepository users;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private ChatModel model;

  @BeforeEach
  void resetLedger() {
    draftLinks.deleteAllInBatch();
    drafts.deleteAllInBatch();
    transactions.deleteAllInBatch();
    identities.deleteAllInBatch();
    users.deleteAllInBatch();
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
  }

  @Test
  void commitsExpenseWithTrustedOwnerSourceAndOriginalMessageDateBeforeConfirming() {
    answer(EXPENSE);

    var reply = chatService.reply(message("1")).text();

    assertThat(reply)
        .contains("Đã ghi", "chi", "45.000đ", "Ăn trưa")
        .doesNotContain("9 triệu", "{");
    var saved = jdbc.queryForMap("SELECT * FROM transactions");
    assertThat(saved.get("amount")).isEqualTo(new BigDecimal("45000.00"));
    assertThat(saved.get("date").toString()).isEqualTo("2026-11-02");
    assertThat(saved.get("type").toString()).isEqualTo("EXPENSE");
    assertThat(saved.get("category").toString()).isEqualTo("FOOD");
    assertThat(saved.get("time_of_day").toString()).isEqualTo("NOON");
    assertThat(saved.get("location")).isEqualTo("Quán cơm");
    assertThat(saved.get("channel").toString()).isEqualTo("TELEGRAM");
    assertThat(saved.get("chat_id")).isEqualTo("123");
    assertThat(saved.get("message_id")).isEqualTo("1");
    assertThat(saved.get("created_at")).isNotNull();
    assertThat(saved.get("updated_at")).isNotNull();
    var identity = jdbc.queryForMap("SELECT * FROM user_identities");
    assertThat(identity.get("external_user_id")).isEqualTo("123");
    assertThat(saved.get("user_id")).isEqualTo(identity.get("user_id"));
    assertThat(users.count()).isOne();
  }

  @Test
  void keepsExplicitIncomeDateAndAllowsMissingOptionalFields() {
    answer(
        """
        {"action":"READY","transaction":{"type":"INCOME","amount":15000000,
        "category":null,"description":null,"location":null,"date":"2026-10-31",
        "timeOfDay":null},"reply":"Khoản thu tiền lương."}
        """);

    var reply = chatService.reply(message("1")).text();

    assertThat(reply).contains("Đã ghi", "thu", "15.000.000đ").doesNotContain("null");
    var saved = transactions.findAll().getFirst();
    assertThat(saved.getType()).isEqualTo(TransactionType.INCOME);
    assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 10, 31));
    assertThat(saved.getCategory()).isNull();
    assertThat(saved.getDescription()).isNull();
  }

  @Test
  void reusesIdentityButRecordsSeparateMessagesWithIdenticalContent() {
    answer(EXPENSE);

    chatService.reply(message("1"));
    chatService.reply(message("2"));

    assertThat(users.count()).isOne();
    assertThat(identities.count()).isOne();
    assertThat(transactions.count()).isEqualTo(2);
  }

  @Test
  void replayKeepsOriginalTransactionAndConfirmsItsStoredValues() {
    answer(EXPENSE);
    chatService.reply(message("1"));
    var originalId = transactions.findAll().getFirst().getId();
    answer(EXPENSE.replace("45000", "99000").replace("Ăn trưa", "Ăn tối"));

    var reply = chatService.reply(message("1")).text();

    assertThat(reply).contains("45.000đ", "Ăn trưa").doesNotContain("99.000đ", "Ăn tối");
    assertThat(transactions.findAll())
        .singleElement()
        .satisfies(saved -> assertThat(saved.getId()).isEqualTo(originalId));
  }

  @Test
  void serializesConcurrentReplayForAnExistingIdentity() throws Exception {
    answer(EXPENSE);
    chatService.reply(message("1"));
    var data =
        new TransactionData(
            TransactionType.EXPENSE, new BigDecimal("20000"), null, null, null, null, null);
    var start = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first =
          executor.submit(() -> {
            start.await();
            return transactionService.record(data, message("2"));
          });
      var second =
          executor.submit(() -> {
            start.await();
            return transactionService.record(data, message("2"));
          });
      start.countDown();

      assertThat(first.get(10, TimeUnit.SECONDS).getId())
          .isEqualTo(second.get(10, TimeUnit.SECONDS).getId());
    }
    assertThat(transactions.count()).isEqualTo(2);
  }

  @Test
  void chatDoesNotWriteAndClarificationPersistsAPendingDraft() {
    answer("{\"action\":\"CHAT\",\"transaction\":null,\"reply\":\"Chào anh!\"}");
    assertThat(chatService.reply(message("1")).text()).isEqualTo("Chào anh!");
    answer(
        """
        {"action":"ASK_CLARIFICATION","transaction":{"type":"EXPENSE","amount":null,
        "category":"FOOD","description":"Ăn trưa","location":null,"date":null,
        "timeOfDay":"NOON"},"reply":"Anh chi bao nhiêu?"}
        """);
    assertThat(chatService.reply(message("2")).text()).isEqualTo("Anh chi bao nhiêu?");
    assertThat(transactions.count()).isZero();
    assertThat(identities.count()).isOne();
    assertThat(users.count()).isOne();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM transaction_drafts", Long.class)).isOne();
    assertThat(jdbc.queryForObject("SELECT status::text FROM transaction_drafts", String.class))
        .isEqualTo("PENDING");
  }

  @Test
  void rejectsUntrustedModelMetadataBeforeWritingAnyRecords() {
    answer(EXPENSE.replace("\"type\":\"EXPENSE\"", "\"userId\":\"attacker\",\"type\":\"EXPENSE\""));

    assertThatThrownBy(() -> chatService.reply(message("1")))
        .isInstanceOfSatisfying(
            ResourceException.class,
            failure ->
                assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_INVALID_RESPONSE));
    assertThat(transactions.count()).isZero();
    assertThat(identities.count()).isZero();
    assertThat(users.count()).isZero();
  }

  @Test
  void databaseFailureRollsBackNewUserAndIdentityAndNeverReturnsConfirmation() {
    answer(EXPENSE);
    jdbc.execute("ALTER TABLE transactions ADD CONSTRAINT test_reject_save CHECK (false)");
    try {
      assertThatThrownBy(() -> chatService.reply(message("1")))
          .isInstanceOfSatisfying(
              ResourceException.class,
              failure ->
                  assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_SAVE_FAILED));
      assertThat(transactions.count()).isZero();
      assertThat(identities.count()).isZero();
      assertThat(users.count()).isZero();
    } finally {
      jdbc.execute("ALTER TABLE transactions DROP CONSTRAINT test_reject_save");
    }
  }

  private static final String ASK =
      """
      {"action":"ASK_CLARIFICATION","transaction":{"type":"EXPENSE","amount":null,
      "category":"FOOD","description":"Ăn trưa","location":"Quán cơm","date":null,
      "timeOfDay":"NOON"},"reply":"Anh chi bao nhiêu?"}
      """;
  private static final String PARTIAL_READY =
      """
      {"action":"READY","transaction":{"type":null,"amount":45000,
      "category":null,"description":null,"location":null,"date":null,
      "timeOfDay":null},"reply":"Khoản chi đã rõ."}
      """;

  @Test
  void preservesOneDraftAcrossRoundsAndConfirmsUsingOriginalDayAndSource() {
    var first = startDraft("10");
    var draftId = first.draftId();
    var before = drafts.findById(draftId).orElseThrow().getUpdatedAt();
    answer(
        """
        {"action":"ASK_CLARIFICATION","transaction":{"type":null,"amount":null,
        "category":null,"description":null,"location":null,"date":null,"timeOfDay":null},
        "reply":"45 nghìn hay 45 triệu anh?"}
        """);
    var secondSource = draftMessage("11", "45", "100");
    var second = chatService.reply(secondSource);
    assertThat(second.draftId()).isEqualTo(draftId);
    chatService.registerQuestion(secondSource, second, "101");
    answer(PARTIAL_READY);
    var finalSource =
        new ChatMessage(
            Channel.TELEGRAM,
            "123",
            "123",
            "12",
            Instant.parse("2026-11-03T05:00:00Z"),
            "45k",
            null);

    var result = chatService.reply(finalSource);

    assertThat(result.draftId()).isNull();
    assertThat(result.text()).contains("Đã ghi", "45.000đ", "Ăn trưa");
    assertThat(drafts.count()).isOne();
    var row = jdbc.queryForMap("SELECT * FROM transaction_drafts");
    assertThat(row.get("status").toString()).isEqualTo("CONFIRMED");
    assertThat(row.get("id")).isEqualTo(draftId);
    var saved = transactions.findAll().getFirst();
    assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(saved.getMessageId()).isEqualTo("10");
    assertThat(saved.getLocation()).isEqualTo("Quán cơm");
    assertThat(saved.getDescription()).isEqualTo("Ăn trưa");
    assertThat(saved.getAmount()).isEqualByComparingTo("45000");
    assertThat(row.get("transaction_id")).isEqualTo(saved.getId());
    assertThat(drafts.findById(draftId).orElseThrow().getUpdatedAt()).isAfter(before);
    assertThat(draftLinks.count()).isEqualTo(5);

    var prompts = ArgumentCaptor.forClass(Prompt.class);
    verify(model, times(3)).call(prompts.capture());
    var last = prompts.getAllValues().getLast();
    assertThat(last.getInstructions().getFirst().getText()).contains("2026-11-01");
    assertThat(last.getInstructions())
        .extracting(message -> message.getText())
        .anySatisfy(text -> assertThat(text).contains("45 nghìn hay 45 triệu"));
    assertThat(last.getInstructions().getLast().getText()).isEqualTo("45k");

    assertThat(chatService.reply(secondSource).text()).contains("Đã ghi", "45.000đ");
    assertThat(chatService.reply(finalSource).text()).contains("Đã ghi", "45.000đ");
    assertThat(chatService.reply(draftMessage("13", "45k", "100")).text()).contains("Đã ghi");
    verify(model, times(3)).call(any(Prompt.class));
    assertThat(transactions.count()).isOne();
  }

  @Test
  void ordinaryChatPreservesPendingDraft() {
    var pending = startDraft("10");
    answer("{\"action\":\"CHAT\",\"transaction\":null,\"reply\":\"Chào anh!\"}");

    assertThat(chatService.reply(draftMessage("11", "Xin chào", null)).text())
        .isEqualTo("Chào anh!");
    assertThat(drafts.findById(pending.draftId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.PENDING);
    assertThat(transactions.count()).isZero();
    assertThat(drafts.count()).isOne();
  }

  @Test
  void cancelledDraftIsRetainedAndOldRepliesCannotReopenItOrAffectANewDraft() {
    var old = startDraft("10");
    var cancelSource = draftMessage("200", "/cancel", null);
    var cancelled = chatService.cancelDraft(cancelSource, old.draftId());
    assertThat(cancelled.status()).isEqualTo(DraftStatus.CANCELLED);
    assertThat(transactions.count()).isZero();
    var next = startDraft("20", "101");

    assertThat(chatService.reply(draftMessage("21", "45k", "100")).text()).contains("đã hủy");
    assertThat(chatService.reply(cancelSource).text()).contains("Đã hủy");
    assertThat(drafts.findById(next.draftId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.PENDING);
    assertThat(drafts.findById(old.draftId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.CANCELLED);
    assertThat(drafts.count()).isEqualTo(2);
    verify(model, times(2)).call(any(Prompt.class));
  }

  @Test
  void cancellationAndContextLookupRespectOwnerAndConversation() {
    var pending = startDraft("10");
    var foreign = new ChatMessage(Channel.TELEGRAM, "456", "456", "1", Instant.EPOCH, "/cancel");
    var wrongChat = new ChatMessage(Channel.TELEGRAM, "123", "456", "1", Instant.EPOCH, "/cancel");
    for (var source : List.of(foreign, wrongChat)) {
      assertThat(draftService.findContext(source)).isEmpty();
      assertThatThrownBy(() -> chatService.cancelDraft(source, pending.draftId()))
          .isInstanceOfSatisfying(
              ResourceException.class,
              error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.DRAFT_NOT_FOUND));
    }
    assertThat(drafts.findById(pending.draftId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.PENDING);
    assertThat(users.count()).isOne();
  }

  @Test
  void staleModelOutputCannotOverwriteANewerQuestionOrConfirmACancelledDraft() {
    var pending = startDraft("10");
    var source = draftMessage("11", "45?", null);
    var stale = draftService.findContext(source).orElseThrow();
    var askAgain =
        new me.nghlong3004.longnest.model.dto.ChatResponse(
            ChatAction.ASK_CLARIFICATION, null, "45 nghìn hay 45 triệu?");
    draftService.apply(askAgain, source, stale);
    var ready =
        new me.nghlong3004.longnest.model.dto.ChatResponse(
            ChatAction.READY,
            new TransactionData(null, new BigDecimal("45000"), null, null, null, null, null),
            "Rõ rồi.");
    var ignored = draftService.apply(ready, draftMessage("12", "45k", null), stale);
    assertThat(ignored.status()).isEqualTo(DraftStatus.PENDING);
    assertThat(ignored.questionText()).isEqualTo("45 nghìn hay 45 triệu?");
    assertThat(ignored.transaction().amount()).isNull();
    assertThat(transactions.count()).isZero();

    chatService.cancelDraft(draftMessage("13", "/cancel", null), pending.draftId());
    assertThat(draftService.apply(ready, draftMessage("14", "45k", null), stale).status())
        .isEqualTo(DraftStatus.CANCELLED);
    assertThat(transactions.count()).isZero();
  }

  @Test
  void failedConfirmationRollsBackEnrichmentAndCanRetryTheSameAnswer() {
    var pending = startDraft("10");
    answer(PARTIAL_READY);
    var source = draftMessage("11", "45k", null);
    jdbc.execute("ALTER TABLE transactions ADD CONSTRAINT test_draft_reject_save CHECK (false)");
    try {
      assertThatThrownBy(() -> chatService.reply(source))
          .isInstanceOfSatisfying(
              ResourceException.class,
              error ->
                  assertThat(error.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_SAVE_FAILED));
      var draft = drafts.findById(pending.draftId()).orElseThrow();
      assertThat(draft.getStatus()).isEqualTo(DraftStatus.PENDING);
      assertThat(draft.getAmount()).isNull();
      assertThat(draftLinks.count()).isEqualTo(2);
      assertThat(transactions.count()).isZero();
    } finally {
      jdbc.execute("ALTER TABLE transactions DROP CONSTRAINT test_draft_reject_save");
    }
    assertThat(chatService.reply(source).text()).contains("Đã ghi", "45.000đ");
    assertThat(transactions.count()).isOne();
    assertThat(draftLinks.count()).isEqualTo(3);
  }

  @Test
  void unknownExplicitReplyDoesNotBecomeANewTransaction() {
    answer(EXPENSE);
    var reply = chatService.reply(draftMessage("10", "45k", "999"));
    assertThat(reply.text()).contains("Không tìm thấy");
    assertThat(transactions.count()).isZero();
    assertThat(drafts.count()).isZero();
    verify(model, never()).call(any(Prompt.class));
  }

  @Test
  void concurrentFinalAnswersCommitOnlyOneTransaction() throws Exception {
    startDraft("10");
    var firstSource = draftMessage("11", "45k", null);
    var context = draftService.findContext(firstSource).orElseThrow();
    var ready =
        new me.nghlong3004.longnest.model.dto.ChatResponse(
            ChatAction.READY,
            new TransactionData(null, new BigDecimal("45000"), null, null, null, null, null),
            "Rõ rồi.");
    var start = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first =
          executor.submit(() -> {
            start.await();
            return draftService.apply(ready, firstSource, context);
          });
      var second =
          executor.submit(() -> {
            start.await();
            return draftService.apply(ready, draftMessage("12", "45k", null), context);
          });
      start.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS).status()).isEqualTo(DraftStatus.CONFIRMED);
      assertThat(second.get(10, TimeUnit.SECONDS).status()).isEqualTo(DraftStatus.CONFIRMED);
    }
    assertThat(transactions.count()).isOne();
    assertThat(drafts.count()).isOne();
    assertThat(draftLinks.count()).isEqualTo(4);
  }

  @Test
  void confirmedDraftCannotBeCancelledAndRecordedSourcesCannotBecomeDraftsOnReplay() {
    var pending = startDraft("10");
    answer(PARTIAL_READY);
    chatService.reply(draftMessage("11", "45k", null));
    assertThat(
            chatService
                .cancelDraft(draftMessage("12", "/cancel", null), pending.draftId())
                .status())
        .isEqualTo(DraftStatus.CONFIRMED);
    answer(ASK);
    assertThat(chatService.reply(draftMessage("10", "Ăn trưa", null)).text()).contains("Đã ghi");
    verify(model, times(2)).call(any(Prompt.class));
    assertThat(drafts.count()).isOne();
    assertThat(transactions.count()).isOne();
  }

  @Test
  void registeringQuestionDeliveryDoesNotDiscardAnAlreadyInterpretedAnswer() {
    answer(ASK);
    var original = draftMessage("10", "Ăn trưa", null);
    var question = chatService.reply(original);
    var answerSource = draftMessage("11", "45k", null);
    var context = draftService.findContext(answerSource).orElseThrow();
    chatService.registerQuestion(original, question, "100");
    var ready =
        new me.nghlong3004.longnest.model.dto.ChatResponse(
            ChatAction.READY,
            new TransactionData(null, new BigDecimal("45000"), null, null, null, null, null),
            "Rõ rồi.");

    assertThat(draftService.apply(ready, answerSource, context).status())
        .isEqualTo(DraftStatus.CONFIRMED);
    assertThat(transactions.count()).isOne();
  }

  @Test
  void directReadyCannotBypassADraftCreatedAfterItsContextWasRead() {
    startDraft("10");
    var ready =
        new TransactionData(
            TransactionType.EXPENSE, new BigDecimal("45000"), null, null, null, null, null);
    assertThatThrownBy(
            () -> transactionService.record(ready, draftMessage("10", "Ăn trưa 45k", null)))
        .isInstanceOf(ResourceException.class);
    assertThat(transactions.count()).isZero();
    assertThat(drafts.findAll())
        .singleElement()
        .satisfies(draft -> assertThat(draft.getStatus()).isEqualTo(DraftStatus.PENDING));
  }

  @Test
  void initialAskCannotCreateADraftForASourceCommittedWhileTheModelWasRunning() {
    var source = draftMessage("10", "Ăn trưa", null);
    transactionService.record(
        new TransactionData(
            TransactionType.EXPENSE, new BigDecimal("45000"), null, null, null, null, null),
        source);
    var ask =
        new me.nghlong3004.longnest.model.dto.ChatResponse(
            ChatAction.ASK_CLARIFICATION, null, "Anh chi bao nhiêu?");
    assertThatThrownBy(() -> draftService.apply(ask, source, null))
        .isInstanceOf(ResourceException.class);
    assertThat(drafts.count()).isZero();
    assertThat(transactions.count()).isOne();
  }

  @Test
  void chatServiceReloadsTheQuestionIfAnotherInterpretationCreatesADraftDuringTheModelCall() {
    var source = draftMessage("10", "Ăn trưa 45k", null);
    when(model.call(any(Prompt.class)))
        .thenAnswer(invocation -> {
          draftService.apply(
              new me.nghlong3004.longnest.model.dto.ChatResponse(
                  ChatAction.ASK_CLARIFICATION, null, "Anh chi bao nhiêu?"),
              source,
              null);
          return new ChatResponse(List.of(new Generation(new AssistantMessage(EXPENSE))));
        });
    var result = chatService.reply(source);
    assertThat(result.draftId()).isNotNull();
    assertThat(result.text()).isEqualTo("Anh chi bao nhiêu?");
    assertThat(transactions.count()).isZero();
    assertThat(drafts.count()).isOne();
    verify(model).call(any(Prompt.class));
  }

  @Test
  void chatServiceReloadsCommittedValuesIfReadyWinsBeforeAnInitialQuestion() {
    var source = draftMessage("10", "Ăn trưa 45k", null);
    when(model.call(any(Prompt.class)))
        .thenAnswer(invocation -> {
          transactionService.record(
              new TransactionData(
                  TransactionType.EXPENSE,
                  new BigDecimal("45000"),
                  null,
                  "Ăn trưa",
                  null,
                  null,
                  null),
              source);
          return new ChatResponse(List.of(new Generation(new AssistantMessage(ASK))));
        });
    var result = chatService.reply(source);
    assertThat(result.draftId()).isNull();
    assertThat(result.text()).contains("Đã ghi", "45.000đ", "Ăn trưa");
    assertThat(transactions.count()).isOne();
    assertThat(drafts.count()).isZero();
    verify(model).call(any(Prompt.class));
  }

  @Test
  void explicitDateCorrectionUsesCurrentReplyDateWithoutChangingOriginalSourceTimestamp() {
    var pending = startDraft("10");
    answer(PARTIAL_READY.replace("\"date\":null", "\"date\":\"2026-11-02\""));
    chatService.reply(
        new ChatMessage(
            Channel.TELEGRAM,
            "123",
            "123",
            "11",
            Instant.parse("2026-11-03T05:00:00Z"),
            "45k, hôm qua nhé",
            null));
    assertThat(transactions.findAll().getFirst().getDate()).isEqualTo(LocalDate.of(2026, 11, 2));
    assertThat(drafts.findById(pending.draftId()).orElseThrow().getOriginalSentAt())
        .isEqualTo(Instant.parse("2026-11-01T16:59:00Z"));
    var prompts = ArgumentCaptor.forClass(Prompt.class);
    verify(model, times(2)).call(prompts.capture());
    assertThat(prompts.getAllValues().getLast().getInstructions().getFirst().getText())
        .contains("Ngày gửi tin nhắn gốc: 2026-11-01", "Ngày gửi tin nhắn mới: 2026-11-03");
  }

  @Test
  void distinctSourceLosingToAPendingDraftRemainsLinkedAfterCancellation() {
    var winnerSource = draftMessage("10", "Ăn trưa", null);
    var losingSource = draftMessage("11", "Ăn trưa 45k", null);
    when(model.call(any(Prompt.class)))
        .thenAnswer(invocation -> {
          draftService.apply(
              new me.nghlong3004.longnest.model.dto.ChatResponse(
                  ChatAction.ASK_CLARIFICATION, null, "Anh chi bao nhiêu?"),
              winnerSource,
              null);
          return new ChatResponse(List.of(new Generation(new AssistantMessage(EXPENSE))));
        });
    var question = chatService.reply(losingSource);
    assertThat(question.draftId()).isNotNull();
    chatService.cancelDraft(draftMessage("200", "/cancel", null), question.draftId());
    answer(EXPENSE);

    assertThat(chatService.reply(losingSource).text()).contains("đã hủy");
    assertThat(transactions.count()).isZero();
    verify(model).call(any(Prompt.class));
  }

  @Test
  void distinctSourceReceiptStillRoutesToTheRecordedDraftAfterConfirmation() {
    var question = startDraft("10");
    var source = draftMessage("11", "Ăn trưa 45k", null);
    var decision =
        transactionService.recordOrContinue(
            new TransactionData(
                TransactionType.EXPENSE, new BigDecimal("45000"), null, null, null, null, null),
            source);
    assertThat(decision.recorded()).isNull();
    assertThat(decision.draft().id()).isEqualTo(question.draftId());
    answer(PARTIAL_READY);
    chatService.reply(draftMessage("12", "45k", null));

    assertThat(chatService.reply(source).text()).contains("Đã ghi", "45.000đ");
    assertThat(transactions.count()).isOne();
    assertThat(transactions.findAll().getFirst().getMessageId()).isEqualTo("10");
    verify(model, times(2)).call(any(Prompt.class));
  }

  private ChatReply startDraft(String id) {
    return startDraft(id, "100");
  }

  private ChatReply startDraft(String id, String questionId) {
    answer(ASK);
    var source = draftMessage(id, "Ăn trưa", null);
    var reply = chatService.reply(source);
    chatService.registerQuestion(source, reply, questionId);
    return reply;
  }

  private static ChatMessage draftMessage(String id, String text, String replyTo) {
    return new ChatMessage(
        Channel.TELEGRAM, "123", "123", id, Instant.parse("2026-11-01T16:59:00Z"), text, replyTo);
  }

  private void answer(String json) {
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));
  }

  private static ChatMessage message(String messageId) {
    return new ChatMessage(
        Channel.TELEGRAM,
        "123",
        "123",
        messageId,
        Instant.parse("2026-11-01T18:30:00Z"),
        "Ăn trưa 45k");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ModelConfiguration {
    @Bean
    ChatClient testChatClient(ChatModel model) {
      return ChatClient.builder(model).build();
    }
  }
}
