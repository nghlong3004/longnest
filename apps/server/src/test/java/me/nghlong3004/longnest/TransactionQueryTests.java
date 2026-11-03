package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionCursor;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.entity.User;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.repository.TransactionDraftRepository;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.repository.UserRepository;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.TransactionMutationService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@Import({TestcontainersConfiguration.class, ChatModelTestConfiguration.class})
class TransactionQueryTests {

  @Autowired private ChatService chatService;
  @Autowired private TransactionQueryService queries;
  @Autowired private TransactionRepository transactions;
  @Autowired private UserRepository users;
  @Autowired private UserIdentityRepository identities;
  @Autowired private TransactionDraftRepository drafts;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionMutationService mutations;
  @MockitoBean private ChatModel model;
  private String queryOwner;

  @BeforeEach
  void resetLedger() {
    queryOwner = UUID.randomUUID().toString();
    jdbc.execute(
        "TRUNCATE deleted_transaction_sources, draft_message_links, transaction_drafts,"
            + " transactions, user_identities, users");
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
  }

  @Test
  void recognizesReadOnlyQueryAndCreatesNoIdentityOrFinancialRows() {
    answer(queryJson("TODAY"));
    var reply = chatService.reply(message("query-1", "Xem khoản chi hôm nay"));

    assertThat(reply.text()).contains("02/11/2026", "Em chưa thấy khoản chi");
    assertThat(reply.transactions().items()).isEmpty();
    assertThat(reply.draftId()).isNull();
    assertThat(users.count()).isZero();
    assertThat(identities.count()).isZero();
    assertThat(transactions.count()).isZero();
    assertThat(drafts.count()).isZero();
  }

  @ParameterizedTest
  @CsvSource({
    "2026-11-01T18:30:00Z,TODAY,2026-11-02,2026-11-02",
    "2026-11-01T18:30:00Z,YESTERDAY,2026-11-01,2026-11-01",
    "2026-11-01T18:30:00Z,SINCE_YESTERDAY,2026-11-01,2026-11-02",
    "2026-11-01T18:30:00Z,THIS_MONTH,2026-11-01,2026-11-02",
    "2026-11-01T18:30:00Z,LAST_MONTH,2026-10-01,2026-10-31",
    "2026-11-01T18:30:00Z,SINCE_LAST_MONTH,2026-10-01,2026-11-02",
    "2025-12-31T18:30:00Z,SINCE_LAST_MONTH,2025-12-01,2026-01-01",
    "2024-02-29T16:59:59Z,SINCE_LAST_MONTH,2024-01-01,2024-02-29",
    "2024-02-29T17:00:00Z,SINCE_LAST_MONTH,2024-02-01,2024-03-01",
    "2025-12-31T18:30:00Z,LAST_MONTH,2025-12-01,2025-12-31",
    "2024-02-29T18:30:00Z,LAST_MONTH,2024-02-01,2024-02-29"
  })
  void resolvesRelativePeriodsAcrossTimeZoneMonthYearAndLeapBoundaries(
      Instant timestamp, QueryPeriod period, LocalDate from, LocalDate to) {
    var source =
        new ChatMessage(Channel.TELEGRAM, queryOwner, "query-chat", "1", timestamp, "Xem chi");
    var page =
        queries.view(
            source, new TransactionQuery(TransactionType.EXPENSE, period, null, null), null);
    assertThat(page.filter().from()).isEqualTo(from);
    assertThat(page.filter().to()).isEqualTo(to);
  }

  @Test
  void includesBothCustomEndpointsAndComputesSinceDateAgainstCurrentMessage() {
    var owner = owner(queryOwner);
    save(owner, TransactionType.EXPENSE, "2026-10-30", "100");
    save(owner, TransactionType.EXPENSE, "2026-11-02", "200");
    save(owner, TransactionType.EXPENSE, "2026-10-29", "400");
    var page =
        queries.view(
            message("1", "Xem chi"),
            new TransactionQuery(
                TransactionType.EXPENSE,
                QueryPeriod.SINCE_DATE,
                LocalDate.parse("2026-10-30"),
                null),
            null);
    assertThat(page.filter().to()).isEqualTo(LocalDate.parse("2026-11-02"));
    assertThat(page.totals().count()).isEqualTo(2);
    assertThat(page.totals().amount()).isEqualByComparingTo("300");
    var custom = queries.view(message("2", "Xem chi"), custom("2026-10-30", "2026-11-02"), null);
    assertThat(custom).isEqualTo(page);
  }

  @Test
  void scopesTotalsToOwnerTypeAndDatesAndPaginatesSameDayTiesWithoutDuplicates() {
    var owner = owner(queryOwner);
    for (int i = 0; i < 12; i++) {
      save(owner, TransactionType.EXPENSE, i < 8 ? "2026-11-02" : "2026-11-01", "1000");
    }
    save(owner, TransactionType.INCOME, "2026-11-02", "999999");
    save(owner, TransactionType.EXPENSE, "2026-10-31", "999999");
    save(owner("other-owner"), TransactionType.EXPENSE, "2026-11-02", "999999");
    var query = custom("2026-11-01", "2026-11-02");
    var first = queries.view(message("1", "Xem chi"), query, null);
    assertThat(first.items()).hasSize(5);
    assertThat(first.totals().count()).isEqualTo(12);
    assertThat(first.totals().amount()).isEqualByComparingTo("12000");
    var second = queries.view(message("1", "Xem chi"), query, first.nextCursor());
    var third = queries.view(message("1", "Xem chi"), query, second.nextCursor());
    assertThat(second.items()).hasSize(5);
    assertThat(third.items()).hasSize(2);
    assertThat(third.nextCursor()).isNull();
    assertThat(second.totals()).isEqualTo(first.totals());
    var ids = new ArrayList<UUID>();
    Stream.of(first, second, third)
        .forEach(page -> page.items().forEach(item -> ids.add(item.id())));
    assertThat(ids).hasSize(12).doesNotHaveDuplicates();
    var income =
        queries.view(
            message("1", "Xem thu"),
            new TransactionQuery(TransactionType.INCOME, QueryPeriod.THIS_MONTH, null, null),
            null);
    assertThat(income.totals().count()).isOne();
    assertThat(income.totals().amount()).isEqualByComparingTo("999999");
    // Even a cursor copied from another owner's list cannot select that owner's rows.
    var foreignCursor =
        new TransactionCursor(first.items().getFirst().date(), first.items().getFirst().id());
    var other =
        new ChatMessage(Channel.TELEGRAM, "nobody", "query-chat", "1", Instant.now(), "Xem chi");
    assertThat(queries.view(other, query, foreignCursor).items()).isEmpty();
    assertThat(users.count()).isEqualTo(2);
  }

  @Test
  void viewingWhileDraftIsPendingPreservesEveryStoredValueAndCanStillCompleteIt() {
    answer(
        """
        {"action":"ASK_CLARIFICATION","transaction":{"type":"EXPENSE","description":"Ăn trưa"},
        "reply":"Anh chi bao nhiêu?"}
        """);
    var original =
        new ChatMessage(
            Channel.TELEGRAM,
            queryOwner,
            "query-chat",
            "10",
            Instant.parse("2026-10-31T05:00:00Z"),
            "Ăn trưa");
    var pending = chatService.reply(original);
    chatService.registerQuestion(original, pending, "100");
    var snapshot = jdbc.queryForMap("SELECT * FROM transaction_drafts");
    var links = jdbc.queryForList("SELECT * FROM draft_message_links ORDER BY id");
    answer(queryJson("TODAY"));
    var result = chatService.reply(message("11", "Xem chi hôm nay"));
    assertThat(result.transactions().filter().from()).isEqualTo(LocalDate.parse("2026-11-02"));
    assertThat(result.draftId()).isNull();
    assertThat(jdbc.queryForMap("SELECT * FROM transaction_drafts")).isEqualTo(snapshot);
    assertThat(jdbc.queryForList("SELECT * FROM draft_message_links ORDER BY id")).isEqualTo(links);
    answer(
        """
        {"action":"READY","transaction":{"type":"EXPENSE","amount":45000},"reply":"Chưa lưu."}
        """);
    assertThat(chatService.reply(message("12", "45k")).text()).contains("Em đã ghi", "45.000đ");
    assertThat(drafts.findById(pending.draftId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.CONFIRMED);
    assertThat(transactions.findAll().getFirst().getDate())
        .isEqualTo(LocalDate.parse("2026-10-31"));
  }

  @Test
  void deletedConfirmedDraftReplaysTruthfullyAndItsOldCancelButtonCannotResurrectIt() {
    answer(
        """
        {"action":"ASK_CLARIFICATION","transaction":{"type":"EXPENSE","description":"Lunch"},"reply":"Amount?"}
        """);
    var original = message("delete-original", "Lunch");
    var draft = chatService.reply(original);
    chatService.registerQuestion(original, draft, "delete-question");
    answer(
        """
        {"action":"READY","transaction":{"amount":45000},"reply":"not trusted"}
        """);
    var completion = message("delete-completion", "45k");
    assertThat(chatService.reply(completion).text()).contains("Em đã ghi");
    var id = transactions.findAll().getFirst().getId();
    mutations.delete(original, id, mutations.load(original, id).version());
    assertThat(chatService.reply(original).text()).contains("đã xóa");
    assertThat(chatService.reply(completion).text()).contains("đã xóa");
    assertThat(
            chatService.cancelDraft(message("delete-cancel", "/cancel"), draft.draftId()).reply())
        .contains("đã xóa");
    assertThat(transactions.count()).isZero();
  }

  @Test
  void queryWorksWhenReplyingToCancelledDraftOrUnrelatedMessage() {
    answer(
        """
        {"action":"ASK_CLARIFICATION","transaction":{"type":"EXPENSE"},"reply":"Bao nhiêu tiền?"}
        """);
    var source = message("10", "Ăn trưa");
    var draft = chatService.reply(source);
    chatService.registerQuestion(source, draft, "100");
    chatService.cancelDraft(message("11", "/cancel"), draft.draftId());
    answer(queryJson("TODAY"));
    for (var target : List.of("100", "999")) {
      var query =
          new ChatMessage(
              Channel.TELEGRAM,
              queryOwner,
              "query-chat",
              "view-" + target,
              source.sentAt(),
              "Xem chi hôm nay",
              target);
      assertThat(chatService.reply(query).transactions()).isNotNull();
    }
    assertThat(drafts.findById(draft.draftId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.CANCELLED);
    assertThat(transactions.count()).isZero();
    answer(
        """
        {"action":"CHAT","transaction":null,"query":null,
        "reply":"Anh muốn xem khoản thu hay chi?"}
        """);
    var ambiguous =
        new ChatMessage(
            Channel.TELEGRAM,
            queryOwner,
            "query-chat",
            "view-ambiguous",
            source.sentAt(),
            "Xem hôm nay",
            "100");
    var continued = chatService.reply(ambiguous);
    assertThat(continued.transactions().filter().type()).isEqualTo(TransactionType.EXPENSE);
    assertThat(continued.text()).contains("02/11/2026");
    assertThat(drafts.findById(draft.draftId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.CANCELLED);
  }

  @Test
  void ambiguousViewingRequestAsksOnlyMissingTypeOnUnrelatedReplyWithoutCreatingDraft() {
    answer(
        """
        {"action":"CHAT","transaction":null,"query":null,
        "reply":"Anh muốn xem khoản thu hay chi và trong khoảng nào?"}
        """);
    var source = message("1", "Xem tháng trước");
    var replied =
        new ChatMessage(
            source.channel(),
            source.externalUserId(),
            source.chatId(),
            source.messageId(),
            source.sentAt(),
            source.text(),
            "999");
    assertThat(chatService.reply(replied).text()).contains("khoản thu hay khoản chi");
    assertThat(drafts.count()).isZero();
    assertThat(transactions.count()).isZero();
  }

  @ParameterizedTest
  @MethodSource("invalidQueries")
  void rejectsInvalidFiltersWithoutCreatingRows(TransactionQuery query) {
    assertThatThrownBy(() -> queries.view(message("1", "Xem chi"), query, null))
        .isInstanceOfSatisfying(
            ResourceException.class,
            error ->
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.QUERY_INVALID_PARAMETERS));
    assertThat(users.count()).isZero();
  }

  static Stream<TransactionQuery> invalidQueries() {
    return Stream.of(
        new TransactionQuery(null, QueryPeriod.TODAY, null, null),
        new TransactionQuery(TransactionType.EXPENSE, null, null, null),
        new TransactionQuery(TransactionType.EXPENSE, QueryPeriod.TODAY, LocalDate.now(), null),
        new TransactionQuery(TransactionType.EXPENSE, QueryPeriod.CUSTOM, null, null),
        new TransactionQuery(TransactionType.EXPENSE, QueryPeriod.SINCE_DATE, null, null),
        custom("2026-11-03", "2026-11-02"),
        custom("0000-01-01", "2026-11-02"));
  }

  @Test
  void rejectsCursorOutsideFilter() {
    assertThatThrownBy(() ->
            queries.view(
                message("1", "Xem chi"),
                custom("2026-11-01", "2026-11-02"),
                new TransactionCursor(LocalDate.parse("2026-10-31"), UUID.randomUUID())))
        .isInstanceOf(ResourceException.class);
  }

  @ParameterizedTest
  @MethodSource("invalidModelQueries")
  void rejectsMalformedQueryOutputBeforeReadingOrWritingFinancialData(String json) {
    answer(json);
    assertThatThrownBy(() -> chatService.reply(message("1", "Xem chi hôm nay")))
        .isInstanceOfSatisfying(
            ResourceException.class,
            error ->
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_INVALID_RESPONSE));
    assertThat(users.count()).isZero();
    assertThat(transactions.count()).isZero();
    assertThat(drafts.count()).isZero();
  }

  static Stream<String> invalidModelQueries() {
    var valid = queryJson("TODAY");
    return Stream.of(
        valid.replace("\"transaction\":null", "\"transaction\":{\"amount\":1000}"),
        valid.replace("TODAY", "TOMORROW"),
        valid.replace("\"type\":\"EXPENSE\"", "\"type\":null"),
        valid.replace("\"action\":\"QUERY\"", "\"action\":\"READY\""),
        valid.replace("\"period\":\"TODAY\"", "\"period\":0"),
        valid.replace("\"from\":null", "\"from\":null,\"userId\":\"other-owner\""),
        "{\"action\":\"QUERY\",\"transaction\":null,\"reply\":\"Xem chi\",\"query\":null}");
  }

  private User owner(String externalId) {
    var user = users.saveAndFlush(User.builder().build());
    identities.saveAndFlush(
        UserIdentity.builder()
            .user(user)
            .channel(Channel.TELEGRAM)
            .externalUserId(externalId)
            .build());
    return user;
  }

  private void save(User owner, TransactionType type, String date, String amount) {
    transactions.saveAndFlush(
        Transaction.builder()
            .user(owner)
            .type(type)
            .amount(new BigDecimal(amount))
            .date(LocalDate.parse(date))
            .channel(Channel.TELEGRAM)
            .chatId("query-chat")
            .messageId(UUID.randomUUID().toString())
            .description("Giao dịch")
            .build());
  }

  private static TransactionQuery custom(String from, String to) {
    return new TransactionQuery(
        TransactionType.EXPENSE, QueryPeriod.CUSTOM, LocalDate.parse(from), LocalDate.parse(to));
  }

  private static String queryJson(String period) {
    return """
    {"action":"QUERY","transaction":null,"reply":"Model bịa tổng 9 triệu!",
    "query":{"type":"EXPENSE","period":"%s","from":null,"to":null}}
    """
        .formatted(period);
  }

  private void answer(String json) {
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));
  }

  private ChatMessage message(String id, String text) {
    return new ChatMessage(
        Channel.TELEGRAM,
        queryOwner,
        "query-chat",
        id,
        Instant.parse("2026-11-01T18:30:00Z"),
        text);
  }
}
