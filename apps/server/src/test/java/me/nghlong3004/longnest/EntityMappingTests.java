package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import me.nghlong3004.longnest.model.entity.Transaction;
import me.nghlong3004.longnest.model.entity.TransactionDraft;
import me.nghlong3004.longnest.model.entity.User;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.main.keep-alive=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class EntityMappingTests {

  @Autowired private EntityManager entityManager;

  @Autowired private JdbcTemplate jdbc;

  @Test
  void discoversEntitiesAndValidatesTheirMappingsAgainstFlywaySchema() {
    assertThat(entityManager.getMetamodel().getEntities())
        .extracting(EntityType::getName)
        .containsExactlyInAnyOrder(
            "User", "UserIdentity", "Transaction", "TransactionDraft", "DraftMessageLink");
  }

  @Test
  void roundTripsIdentityTransactionAndConfirmedDraftWithNativeEnumsAndLazyRelations() {
    var user = User.builder().build();
    entityManager.persist(user);
    var identity =
        UserIdentity.builder()
            .user(user)
            .channel(Channel.TELEGRAM)
            .externalUserId("telegram-user-1")
            .build();
    entityManager.persist(identity);
    var transaction = newTransaction(user);
    entityManager.persist(transaction);
    var draft = newDraft(user);
    draft.askForClarification("Bao nhiêu tiền?", "question-1");
    draft.confirm(transaction);
    entityManager.persist(draft);
    entityManager.flush();
    entityManager.clear();

    var storedIdentity = entityManager.find(UserIdentity.class, identity.getId());
    assertThat(storedIdentity.getChannel()).isEqualTo(Channel.TELEGRAM);
    assertThat(storedIdentity.getExternalUserId()).isEqualTo("telegram-user-1");
    assertThat(Hibernate.isInitialized(storedIdentity.getUser())).isFalse();
    assertThat(storedIdentity.getUser().getId()).isEqualTo(user.getId());
    assertThat(storedIdentity.getCreatedAt()).isNotNull();

    var storedDraft = entityManager.find(TransactionDraft.class, draft.getId());
    assertThat(storedDraft.getStatus()).isEqualTo(DraftStatus.CONFIRMED);
    assertThat(storedDraft.getAmount()).isNull();
    assertThat(storedDraft.getOriginalText()).isEqualTo("Ăn trưa hôm qua");
    assertThat(storedDraft.getQuestionMessageId()).isEqualTo("question-1");
    assertThat(Hibernate.isInitialized(storedDraft.getTransaction())).isFalse();

    var storedTransaction = storedDraft.getTransaction();
    assertThat(storedTransaction.getId()).isEqualTo(transaction.getId());
    assertThat(storedTransaction.getType()).isEqualTo(TransactionType.EXPENSE);
    assertThat(storedTransaction.getCategory()).isEqualTo(TransactionCategory.FOOD);
    assertThat(storedTransaction.getTimeOfDay()).isEqualTo(TimeOfDay.NOON);
    assertThat(storedTransaction.getChannel()).isEqualTo(Channel.TELEGRAM);
    assertThat(storedTransaction.getAmount()).isEqualByComparingTo("45000");
    assertThat(storedTransaction.getDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(storedTransaction.getCreatedAt()).isNotNull();
    assertThat(storedTransaction.getUpdatedAt()).isNotNull();
  }

  @Test
  void retainsCancelledDraftAndAutomaticallyUpdatesAuditTimestamp() {
    var user = User.builder().build();
    entityManager.persist(user);
    var draft = newDraft(user);
    entityManager.persist(draft);
    entityManager.flush();
    var createdAt = draft.getCreatedAt();
    jdbc.update(
        "UPDATE transaction_drafts SET updated_at = TIMESTAMPTZ '2000-01-01 00:00:00+00' WHERE id ="
            + " ?",
        draft.getId());
    entityManager.clear();

    var storedDraft = entityManager.find(TransactionDraft.class, draft.getId());
    storedDraft.askForClarification("Bao nhiêu tiền?", "first-question");
    storedDraft.askForClarification("Anh nói rõ số tiền nhé?", "second-question");
    storedDraft.cancel();
    entityManager.flush();
    entityManager.clear();

    var cancelledDraft = entityManager.find(TransactionDraft.class, draft.getId());
    assertThat(cancelledDraft.getStatus()).isEqualTo(DraftStatus.CANCELLED);
    assertThat(cancelledDraft.getQuestionMessageId()).isEqualTo("second-question");
    assertThat(cancelledDraft.getOriginalText()).isEqualTo("Ăn trưa hôm qua");
    assertThat(cancelledDraft.getCreatedAt()).isEqualTo(createdAt);
    assertThat(cancelledDraft.getUpdatedAt()).isAfter(Instant.parse("2000-01-01T00:00:00Z"));
    assertThatThrownBy(() -> cancelledDraft.askForClarification("Mở lại?", "third-question"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void preventsConfirmationForAnotherOwnerAndChangesToFinishedDrafts() {
    var user = User.builder().build();
    var otherUser = User.builder().build();
    entityManager.persist(user);
    entityManager.persist(otherUser);
    var transaction = newTransaction(user);
    entityManager.persist(transaction);
    var otherDraft = newDraft(otherUser);

    assertThatThrownBy(() -> otherDraft.confirm(transaction))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(otherDraft.getStatus()).isEqualTo(DraftStatus.PENDING);
    assertThat(otherDraft.getTransaction()).isNull();

    var draft = newDraft(user);
    draft.confirm(transaction);
    assertThatThrownBy(draft::cancel).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> draft.confirm(transaction)).isInstanceOf(IllegalStateException.class);
    assertThat(draft.getStatus()).isEqualTo(DraftStatus.CONFIRMED);

    otherDraft.cancel();
    assertThatThrownBy(() -> otherDraft.confirm(transaction))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsInvalidMoneyAtConstructionInsteadOfRoundingItSilently() {
    var user = User.builder().build();
    assertThatThrownBy(() ->
            Transaction.builder()
                .user(user)
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.ZERO)
                .build())
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() ->
            TransactionDraft.builder()
                .user(user)
                .channel(Channel.TELEGRAM)
                .chatId("chat-1")
                .messageId("message-1")
                .originalText("Ăn trưa")
                .amount(new BigDecimal("0.001"))
                .build())
        .isInstanceOf(ArithmeticException.class);
  }

  private static Transaction newTransaction(User user) {
    return Transaction.builder()
        .user(user)
        .type(TransactionType.EXPENSE)
        .amount(new BigDecimal("45000"))
        .category(TransactionCategory.FOOD)
        .description("Ăn trưa")
        .location("Quán cơm")
        .date(LocalDate.of(2026, 11, 1))
        .timeOfDay(TimeOfDay.NOON)
        .channel(Channel.TELEGRAM)
        .chatId("chat-1")
        .messageId("message-1")
        .build();
  }

  private static TransactionDraft newDraft(User user) {
    return TransactionDraft.builder()
        .user(user)
        .channel(Channel.TELEGRAM)
        .chatId("chat-1")
        .messageId("message-1")
        .originalText("Ăn trưa hôm qua")
        .originalSentAt(Instant.parse("2026-11-01T05:00:00Z"))
        .build();
  }
}
