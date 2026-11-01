package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.repository.UserRepository;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  @Autowired private TransactionService transactionService;
  @Autowired private TransactionRepository transactions;
  @Autowired private UserIdentityRepository identities;
  @Autowired private UserRepository users;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private ChatModel model;

  @BeforeEach
  void resetLedger() {
    transactions.deleteAllInBatch();
    identities.deleteAllInBatch();
    users.deleteAllInBatch();
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
  }

  @Test
  void commitsExpenseWithTrustedOwnerSourceAndOriginalMessageDateBeforeConfirming() {
    answer(EXPENSE);

    var reply = chatService.reply(message("1"));

    assertThat(reply).contains("Đã ghi", "chi", "45.000đ", "Ăn trưa").doesNotContain("9 triệu", "{");
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

    var reply = chatService.reply(message("1"));

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

    var reply = chatService.reply(message("1"));

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
          executor.submit(
              () -> {
                start.await();
                return transactionService.record(data, message("2"));
              });
      var second =
          executor.submit(
              () -> {
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
  void chatAndClarificationReturnOnlyReplyWithoutCreatingAnyLedgerRecords() {
    answer("{\"action\":\"CHAT\",\"transaction\":null,\"reply\":\"Chào anh!\"}");
    assertThat(chatService.reply(message("1"))).isEqualTo("Chào anh!");
    answer(
        """
        {"action":"ASK_CLARIFICATION","transaction":{"type":"EXPENSE","amount":null,
        "category":"FOOD","description":"Ăn trưa","location":null,"date":null,
        "timeOfDay":"NOON"},"reply":"Anh chi bao nhiêu?"}
        """);
    assertThat(chatService.reply(message("2"))).isEqualTo("Anh chi bao nhiêu?");
    assertThat(transactions.count()).isZero();
    assertThat(identities.count()).isZero();
    assertThat(users.count()).isZero();
  }

  @Test
  void rejectsUntrustedModelMetadataBeforeWritingAnyRecords() {
    answer(
        EXPENSE.replace(
            "\"type\":\"EXPENSE\"", "\"userId\":\"attacker\",\"type\":\"EXPENSE\""));

    assertThatThrownBy(() -> chatService.reply(message("1")))
        .isInstanceOfSatisfying(
            ResourceException.class,
            failure -> assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_INVALID_RESPONSE));
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
              failure -> assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_SAVE_FAILED));
      assertThat(transactions.count()).isZero();
      assertThat(identities.count()).isZero();
      assertThat(users.count()).isZero();
    } finally {
      jdbc.execute("ALTER TABLE transactions DROP CONSTRAINT test_reject_save");
    }
  }

  private void answer(String json) {
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));
  }

  private static ChatMessage message(String messageId) {
    return new ChatMessage(
        Channel.TELEGRAM, "123", "123", messageId,
        Instant.parse("2026-11-01T18:30:00Z"), "Ăn trưa 45k");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ModelConfiguration {
    @Bean
    ChatClient testChatClient(ChatModel model) {
      return ChatClient.builder(model).build();
    }
  }
}
