package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class DatabaseMigrationTests {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-bookworm");

  private static Flyway flyway;
  private static JdbcTemplate jdbc;

  @BeforeAll
  static void migrate() {
    var dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    flyway = Flyway.configure().dataSource(dataSource).load();
    flyway.migrate();
    jdbc = new JdbcTemplate(dataSource);
  }

  @Test
  void linksMultipleIdentitiesToOneUserAndRejectsDuplicateExternalIdentity() {
    var userId = newUser();
    var externalId = UUID.randomUUID().toString();
    jdbc.update(
        """
        INSERT INTO user_identities (user_id, channel, external_user_id)
        VALUES (?, 'TELEGRAM', ?), (?, 'TELEGRAM', ?)
        """,
        userId, externalId, userId, UUID.randomUUID().toString());

    assertThat(
            jdbc.queryForList(
                "SELECT user_id FROM user_identities WHERE user_id = ?", UUID.class, userId))
        .containsExactly(userId, userId);
    assertConstraint(
        "23505",
        () ->
            jdbc.update(
                """
                INSERT INTO user_identities (user_id, channel, external_user_id)
                VALUES (?, 'TELEGRAM', ?)
                """,
                newUser(), externalId));
    assertConstraint(
        "23503",
        () ->
            jdbc.update(
                """
                INSERT INTO user_identities (user_id, channel, external_user_id)
                VALUES (?, 'TELEGRAM', ?)
                """,
                UUID.randomUUID(), UUID.randomUUID().toString()));
  }

  @Test
  void recordsMultipleExpensesFromOneMessageWithoutRequiringOptionalDetails() {
    var userId = newUser();
    var transactionId = newTransaction(userId);
    newTransaction(userId);

    var transaction =
        jdbc.queryForMap("SELECT * FROM transactions WHERE id = ?", transactionId);
    assertThat(transaction.get("amount")).isEqualTo(new BigDecimal("45000.00"));
    assertThat(transaction.get("category")).isNull();
    assertThat(transaction.get("time_of_day")).isNull();
    assertThat(transaction.get("created_at")).isNotNull();
    assertThat(transaction.get("updated_at")).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM transactions WHERE user_id = ?", Long.class, userId))
        .isEqualTo(2);
  }

  @Test
  void rejectsNonPositiveOrNonFiniteMoneyAndInvalidTransactionEnums() {
    var transactionId = newTransaction(newUser());
    for (var amount : new String[] {"0", "-1", "NaN"}) {
      assertConstraint(
          "23514",
          () ->
              jdbc.update(
                  "UPDATE transactions SET amount = CAST(? AS numeric) WHERE id = ?",
                  amount, transactionId));
    }
    assertConstraint(
        "23502",
        () -> jdbc.update("UPDATE transactions SET amount = NULL WHERE id = ?", transactionId));
    assertConstraint(
        "22P02",
        () -> jdbc.update("UPDATE transactions SET type = 'OUTCOME' WHERE id = ?", transactionId));
    assertConstraint(
        "22P02",
        () ->
            jdbc.update(
                "UPDATE transactions SET time_of_day = 'UNKNOWN' WHERE id = ?", transactionId));
  }

  @Test
  void retainsIncompleteDraftThroughClarificationsAndCancellation() {
    var draftId = newDraft(newUser());
    assertThat(
            jdbc.queryForObject(
                "SELECT amount FROM transaction_drafts WHERE id = ?", BigDecimal.class, draftId))
        .isNull();

    jdbc.update(
        """
        UPDATE transaction_drafts
        SET amount = 45000, question_text = 'Hôm nay hay hôm qua?',
            question_message_id = 'next-question', updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """,
        draftId);
    jdbc.update("UPDATE transaction_drafts SET status = 'CANCELLED' WHERE id = ?", draftId);

    var draft = jdbc.queryForMap("SELECT * FROM transaction_drafts WHERE id = ?", draftId);
    assertThat(draft.get("status")).isEqualTo("CANCELLED");
    assertThat(draft.get("amount")).isEqualTo(new BigDecimal("45000.00"));
    assertThat(draft.get("original_text")).isEqualTo("Ăn trưa hôm qua");
    assertThat(draft.get("question_message_id")).isEqualTo("next-question");
    assertThat(draft.get("transaction_id")).isNull();
    assertConstraint(
        "23514",
        () ->
            jdbc.update(
                "UPDATE transaction_drafts SET status = 'CONFIRMED' WHERE id = ?", draftId));
    assertConstraint(
        "22P02",
        () -> jdbc.update("UPDATE transaction_drafts SET status = 'UNKNOWN' WHERE id = ?", draftId));
    assertConstraint(
        "23514",
        () -> jdbc.update("UPDATE transaction_drafts SET amount = -1 WHERE id = ?", draftId));
  }

  @Test
  void onlyConfirmsDraftAgainstOneTransactionOwnedByTheSameUser() {
    var ownerId = newUser();
    var transactionId = newTransaction(ownerId);
    var otherDraftId = newDraft(newUser());
    assertConstraint(
        "23503",
        () ->
            jdbc.update(
                """
                UPDATE transaction_drafts SET status = 'CONFIRMED', transaction_id = ?
                WHERE id = ?
                """,
                transactionId, otherDraftId));

    var draftId = newDraft(ownerId);
    assertConstraint(
        "23514",
        () ->
            jdbc.update(
                "UPDATE transaction_drafts SET transaction_id = ? WHERE id = ?",
                transactionId, draftId));
    jdbc.update(
        """
        UPDATE transaction_drafts SET status = 'CONFIRMED', transaction_id = ?
        WHERE id = ?
        """,
        transactionId, draftId);
    assertThat(
            jdbc.queryForObject(
                "SELECT transaction_id FROM transaction_drafts WHERE id = ?", UUID.class, draftId))
        .isEqualTo(transactionId);

    var duplicateDraftId = newDraft(ownerId);
    assertConstraint(
        "23505",
        () ->
            jdbc.update(
                """
                UPDATE transaction_drafts SET status = 'CONFIRMED', transaction_id = ?
                WHERE id = ?
                """,
                transactionId, duplicateDraftId));
  }

  @Test
  void validatesHistoryAndDoesNotReapplyTheInitialMigration() {
    flyway.validate();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(flyway.info().current()).isNotNull();
  }

  private static UUID newUser() {
    return jdbc.queryForObject("INSERT INTO users DEFAULT VALUES RETURNING id", UUID.class);
  }

  private static UUID newTransaction(UUID userId) {
    return jdbc.queryForObject(
        """
        INSERT INTO transactions (user_id, type, amount, date, channel, chat_id, message_id)
        VALUES (?, 'EXPENSE', 45000, DATE '2026-11-01', 'TELEGRAM', 'chat-1', 'message-1')
        RETURNING id
        """,
        UUID.class, userId);
  }

  private static UUID newDraft(UUID userId) {
    return jdbc.queryForObject(
        """
        INSERT INTO transaction_drafts (user_id, channel, chat_id, message_id, original_text)
        VALUES (?, 'TELEGRAM', 'chat-1', ?, 'Ăn trưa hôm qua')
        RETURNING id
        """,
        UUID.class, userId, UUID.randomUUID().toString());
  }

  private static void assertConstraint(String sqlState, ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOf(DataIntegrityViolationException.class)
        .rootCause()
        .isInstanceOf(SQLException.class)
        .extracting(cause -> ((SQLException) cause).getSQLState())
        .isEqualTo(sqlState);
  }
}
