package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.entity.DeletedTransactionSource;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.entity.TransactionActionInput;
import me.nghlong3004.longnest.model.entity.User;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class TransactionInvariantTests {
  static Stream<TransactionData> invalidRevisions() {
    return Stream.of(
        data(null, "2", LocalDate.of(2026, 11, 2), null),
        data(TransactionType.INCOME, "0", LocalDate.of(2026, 11, 2), null),
        data(TransactionType.INCOME, "-1", LocalDate.of(2026, 11, 2), null),
        data(TransactionType.INCOME, "0.001", LocalDate.of(2026, 11, 2), null),
        data(TransactionType.INCOME, "100000000000000000", LocalDate.of(2026, 11, 2), null),
        data(TransactionType.INCOME, null, LocalDate.of(2026, 11, 2), null),
        data(TransactionType.INCOME, "2", null, null),
        data(TransactionType.INCOME, "2", LocalDate.of(2026, 11, 2), TransactionCategory.FOOD));
  }

  @ParameterizedTest
  @MethodSource("invalidRevisions")
  void rejectsInvalidRevisionBeforeChangingAnyField(TransactionData proposed) {
    var row = transaction();
    var id = UUID.randomUUID();
    var owner = row.getUser();
    ReflectionTestUtils.setField(row, "id", id);
    ReflectionTestUtils.setField(row, "version", 7L);
    assertThatThrownBy(() -> row.revise(proposed)).isInstanceOf(RuntimeException.class);
    assertThat(row.getType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(row.getAmount()).isEqualByComparingTo("45000");
    assertThat(row.getCategory()).isEqualTo(TransactionCategory.FOOD);
    assertThat(row.getDescription()).isEqualTo("legacy".repeat(200));
    assertThat(row.getLocation()).isEqualTo("old");
    assertThat(row.getDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(row.getTimeOfDay()).isEqualTo(TimeOfDay.NOON);
    assertThat(row.getChatId()).isEqualTo("chat");
    assertThat(row.getMessageId()).isEqualTo("message");
    assertThat(row.getId()).isEqualTo(id);
    assertThat(row.getUser()).isSameAs(owner);
    assertThat(row.getChannel()).isEqualTo(Channel.TELEGRAM);
    assertThat(row.getVersion()).isEqualTo(7L);
    assertThat(row.getCreatedAt()).isNull();
    assertThat(row.getUpdatedAt()).isNull();
  }

  @ParameterizedTest
  @MethodSource("invalidRevisions")
  void builderUsesTheSameRequiredAndMoneyInvariants(TransactionData data) {
    assertThatThrownBy(() ->
            Transaction.builder()
                .user(User.builder().build())
                .type(data.type())
                .amount(data.amount())
                .category(data.category())
                .date(data.date())
                .channel(Channel.TELEGRAM)
                .chatId("chat")
                .messageId("message")
                .build())
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void revisionNormalizesMoneyWithoutLosingLegacyTextOrSource() {
    var row = transaction();
    row.revise(
        new TransactionData(
            row.getType(),
            new BigDecimal("12.5"),
            row.getCategory(),
            row.getDescription(),
            row.getLocation(),
            row.getDate(),
            row.getTimeOfDay()));
    assertThat(row.getAmount()).isEqualTo(new BigDecimal("12.50"));
    assertThat(row.getDescription()).hasSize(1200);
    assertThat(row.getChannel()).isEqualTo(Channel.TELEGRAM);
    assertThat(row.getMessageId()).isEqualTo("message");
  }

  @Test
  void receiptsRejectMissingSourceAndOwnerWithoutAllowingIdInjection() {
    assertThatThrownBy(() -> DeletedTransactionSource.builder().build())
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() ->
            DeletedTransactionSource.builder()
                .userId(UUID.randomUUID())
                .channel(Channel.TELEGRAM)
                .chatId(" ")
                .messageId("1")
                .build())
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> TransactionActionInput.builder().build())
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() ->
            TransactionActionInput.builder()
                .channel(Channel.TELEGRAM)
                .externalUserId(" ")
                .chatId("chat")
                .messageId("1")
                .build())
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(
            DeletedTransactionSource.builder()
                .userId(UUID.randomUUID())
                .channel(Channel.TELEGRAM)
                .chatId("chat")
                .messageId("1")
                .build()
                .getId())
        .isNull();
    assertThat(
            TransactionActionInput.builder()
                .channel(Channel.TELEGRAM)
                .externalUserId("user")
                .chatId("chat")
                .messageId("1")
                .build()
                .getId())
        .isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"owner", "channel", "chat", "message"})
  void deletionReceiptRequiresEverySourceField(String missing) {
    assertThatThrownBy(() ->
            DeletedTransactionSource.builder()
                .userId(missing.equals("owner") ? null : UUID.randomUUID())
                .channel(missing.equals("channel") ? null : Channel.TELEGRAM)
                .chatId(missing.equals("chat") ? " " : "chat")
                .messageId(missing.equals("message") ? " " : "message")
                .build())
        .isInstanceOf(RuntimeException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"owner", "channel", "chat", "message"})
  void consumedInputReceiptRequiresEverySourceField(String missing) {
    assertThatThrownBy(() ->
            TransactionActionInput.builder()
                .externalUserId(missing.equals("owner") ? " " : "user")
                .channel(missing.equals("channel") ? null : Channel.TELEGRAM)
                .chatId(missing.equals("chat") ? " " : "chat")
                .messageId(missing.equals("message") ? " " : "message")
                .build())
        .isInstanceOf(RuntimeException.class);
  }

  private static TransactionData data(
      TransactionType type, String amount, LocalDate date, TransactionCategory category) {
    return new TransactionData(
        type,
        amount == null ? null : new BigDecimal(amount),
        category,
        "new",
        "new location",
        date,
        TimeOfDay.EVENING);
  }

  private static Transaction transaction() {
    return Transaction.builder()
        .user(User.builder().build())
        .type(TransactionType.EXPENSE)
        .amount(new BigDecimal("45000"))
        .category(TransactionCategory.FOOD)
        .description("legacy".repeat(200))
        .location("old")
        .date(LocalDate.of(2026, 11, 1))
        .timeOfDay(TimeOfDay.NOON)
        .channel(Channel.TELEGRAM)
        .chatId("chat")
        .messageId("message")
        .build();
  }
}
