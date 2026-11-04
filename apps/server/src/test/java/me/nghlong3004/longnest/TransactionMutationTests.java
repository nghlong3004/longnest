package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.entity.TransactionDraft;
import me.nghlong3004.longnest.model.entity.User;
import me.nghlong3004.longnest.model.entity.UserIdentity;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.model.enums.TimeOfDay;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionField;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.repository.TransactionDraftRepository;
import me.nghlong3004.longnest.repository.TransactionRepository;
import me.nghlong3004.longnest.repository.UserIdentityRepository;
import me.nghlong3004.longnest.repository.UserRepository;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionMutationService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import me.nghlong3004.longnest.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.main.keep-alive=false",
      "longnest.telegram.enabled=false",
      "longnest.deepseek.enabled=false"
    })
@Import(TestcontainersConfiguration.class)
class TransactionMutationTests {
  @Autowired TransactionMutationService mutations;
  @Autowired TransactionService recording;
  @Autowired TransactionQueryService queries;
  @Autowired TransactionRepository transactions;
  @Autowired TransactionDraftRepository drafts;
  @Autowired UserRepository users;
  @Autowired UserIdentityRepository identities;
  @Autowired JdbcTemplate jdbc;
  @Autowired DraftService draftService;
  ChatMessage source;
  UUID id;

  @BeforeEach
  void setup() {
    var external = UUID.randomUUID().toString();
    source =
        new ChatMessage(
            Channel.TELEGRAM,
            external,
            "mutation-chat",
            UUID.randomUUID().toString(),
            Instant.now(),
            "synthetic");
    var user = users.saveAndFlush(User.builder().build());
    identities.saveAndFlush(
        UserIdentity.builder()
            .user(user)
            .channel(Channel.TELEGRAM)
            .externalUserId(external)
            .build());
    id = recording.record(data(), source).getId();
  }

  @Test
  void editsOnlySelectedFieldPreservesProvenanceAndRejectsStaleSave() {
    var original = jdbc.queryForMap("select * from transactions where id = ?", id);
    var before = mutations.load(source, id);
    var proposed = mutations.preview(before, TransactionField.AMOUNT, "123.45");
    assertThat(transactions.findById(id).orElseThrow().getAmount()).isEqualByComparingTo("45000");
    mutations.save(source, id, before.version(), proposed);
    var after = jdbc.queryForMap("select * from transactions where id = ?", id);
    for (var key : original.keySet()) {
      if (!java.util.Set.of("amount", "updated_at", "version").contains(key))
        assertThat(after.get(key)).as(key).isEqualTo(original.get(key));
    }
    assertThat(after.get("amount")).isEqualTo(new BigDecimal("123.45"));
    assertThatThrownBy(() -> mutations.save(source, id, before.version(), proposed))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> mutations.delete(source, id, before.version()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deniesForeignOwnerAndChatOnEveryDatabaseStep() {
    var before = mutations.load(source, id);
    for (var foreign :
        java.util.List.of(
            new ChatMessage(
                Channel.TELEGRAM, "unknown-owner", source.chatId(), "1", Instant.now(), "x"),
            new ChatMessage(
                Channel.TELEGRAM,
                source.externalUserId(),
                "other-chat",
                "1",
                Instant.now(),
                "x"))) {
      assertThatThrownBy(() -> mutations.load(foreign, id))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> mutations.save(foreign, id, before.version(), data()))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> mutations.delete(foreign, id, before.version()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(transactions.existsById(id)).isTrue();
  }

  @Test
  void validatesStrictLiteralMoneyDatesEnumsAndTextWithoutWriting() {
    var before = mutations.load(source, id);
    for (var invalid :
        java.util.List.of("0", "-1", "1e3", "NaN", "1,000", "1.001", "100000000000000000")) {
      assertThatThrownBy(() -> mutations.preview(before, TransactionField.AMOUNT, invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }
    for (var invalid : java.util.List.of("2026-02-30", "0000-01-01", "tomorrow")) {
      assertThatThrownBy(() -> mutations.preview(before, TransactionField.DATE, invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThatThrownBy(() -> mutations.preview(before, TransactionField.TYPE, "DELETE"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> mutations.preview(before, null, "hacked"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> mutations.preview(before, TransactionField.DESCRIPTION, "x".repeat(501)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(mutations.load(source, id)).isEqualTo(before);
  }

  @Test
  void acceptsUnambiguousThousandsAndPreservesUntouchedLongText() {
    jdbc.update(
        "update transactions set description = ?, location = ? where id = ?",
        "d".repeat(2000),
        "l".repeat(1000),
        id);
    var before = mutations.load(source, id);
    assertThatCode(() -> mutations.preview(before, TransactionField.AMOUNT, "50k"))
        .doesNotThrowAnyException();
    var data = mutations.preview(before, TransactionField.AMOUNT, "50k");
    assertThat(data.amount()).isEqualByComparingTo("50000");
    mutations.save(source, id, before.version(), data);
    var after = transactions.findById(id).orElseThrow();
    assertThat(after.getDescription()).hasSize(2000);
    assertThat(after.getLocation()).hasSize(1000);
    assertThat(after.getAmount()).isEqualByComparingTo("50000");
  }

  @Test
  void typedNullableFieldsPreserveAllOtherValues() {
    var before = mutations.load(source, id);
    assertThat(mutations.preview(before, TransactionField.CATEGORY, "-"))
        .isEqualTo(
            new TransactionData(
                TransactionType.EXPENSE,
                new BigDecimal("45000.00"),
                null,
                "Lunch",
                "Cafe",
                LocalDate.of(2026, 11, 2),
                TimeOfDay.NOON));
    assertThat(mutations.preview(before, TransactionField.DESCRIPTION, "-"))
        .isEqualTo(
            new TransactionData(
                TransactionType.EXPENSE,
                new BigDecimal("45000.00"),
                TransactionCategory.FOOD,
                null,
                "Cafe",
                LocalDate.of(2026, 11, 2),
                TimeOfDay.NOON));
    assertThat(mutations.preview(before, TransactionField.LOCATION, "-"))
        .isEqualTo(
            new TransactionData(
                TransactionType.EXPENSE,
                new BigDecimal("45000.00"),
                TransactionCategory.FOOD,
                "Lunch",
                null,
                LocalDate.of(2026, 11, 2),
                TimeOfDay.NOON));
    assertThat(mutations.preview(before, TransactionField.TIME_OF_DAY, "EVENING"))
        .isEqualTo(
            new TransactionData(
                TransactionType.EXPENSE,
                new BigDecimal("45000.00"),
                TransactionCategory.FOOD,
                "Lunch",
                "Cafe",
                LocalDate.of(2026, 11, 2),
                TimeOfDay.EVENING));
    assertThat(mutations.load(source, id)).isEqualTo(before);
  }

  @Test
  void deletesConfirmedDraftAndDirectRowsWithoutSourceResurrection() {
    var saved = transactions.findById(id).orElseThrow();
    var draft =
        TransactionDraft.builder()
            .user(saved.getUser())
            .type(TransactionType.EXPENSE)
            .amount(data().amount())
            .date(data().date())
            .channel(source.channel())
            .chatId(source.chatId())
            .messageId(source.messageId())
            .originalText("synthetic")
            .originalSentAt(source.sentAt())
            .build();
    draft = drafts.saveAndFlush(draft);
    draft.confirm(saved);
    drafts.saveAndFlush(draft);
    jdbc.update(
        "insert into draft_message_links(id, draft_id, channel, chat_id, message_id) values(?, ?,"
            + " 'TELEGRAM', ?, ?)",
        UUID.randomUUID(),
        draft.getId(),
        source.chatId(),
        "confirmed-followup-" + id);
    var before = mutations.load(source, id);
    mutations.delete(source, id, before.version());
    assertThat(transactions.existsById(id)).isFalse();
    assertThat(drafts.findById(draft.getId()).orElseThrow().getStatus())
        .isEqualTo(DraftStatus.DELETED);
    assertThat(drafts.findById(draft.getId()).orElseThrow().getAmount()).isNull();
    var replay =
        new ChatMessage(
            source.channel(),
            source.externalUserId(),
            source.chatId(),
            "confirmed-followup-" + id,
            source.sentAt(),
            "50k");
    assertThat(draftService.findContext(replay).orElseThrow().status())
        .isEqualTo(DraftStatus.DELETED);
    assertThatThrownBy(() -> recording.record(data(), source)).isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> recording.recordOrContinue(data(), source))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> recording.findRecorded(source)).isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> mutations.delete(source, id, before.version()))
        .isInstanceOf(IllegalArgumentException.class);
    var directSource =
        new ChatMessage(
            source.channel(),
            source.externalUserId(),
            source.chatId(),
            UUID.randomUUID().toString(),
            source.sentAt(),
            "direct");
    var direct = recording.record(data(), directSource);
    mutations.delete(directSource, direct.getId(), 0);
    assertThatThrownBy(() -> recording.record(data(), directSource))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void changesFilterMembershipTotalsAndFreshPagination() {
    for (int i = 0; i < 6; i++)
      recording.record(
          data(),
          new ChatMessage(
              source.channel(),
              source.externalUserId(),
              source.chatId(),
              UUID.randomUUID().toString(),
              source.sentAt(),
              "x"));
    var query =
        new TransactionQuery(
            TransactionType.EXPENSE, QueryPeriod.CUSTOM, data().date(), data().date());
    var first = queries.view(source, query, null);
    assertThat(first.totals().count()).isEqualTo(7);
    assertThat(first.nextCursor()).isNotNull();
    var before = mutations.load(source, id);
    mutations.save(
        source, id, before.version(), mutations.preview(before, TransactionField.TYPE, "INCOME"));
    assertThat(queries.view(source, query, null).totals().count()).isEqualTo(6);
    var other =
        first.items().stream().filter(item -> !item.id().equals(id)).findFirst().orElseThrow();
    before = mutations.load(source, other.id());
    mutations.save(
        source,
        other.id(),
        before.version(),
        mutations.preview(before, TransactionField.DATE, "2026-10-01"));
    var refreshed = queries.view(source, query, null);
    assertThat(refreshed.totals().amount()).isEqualByComparingTo("225000");
    assertThat(refreshed.items()).hasSize(5);
    assertThat(refreshed.nextCursor()).isNull();
  }

  private TransactionData data() {
    return new TransactionData(
        TransactionType.EXPENSE,
        new BigDecimal("45000"),
        TransactionCategory.FOOD,
        "Lunch",
        "Cafe",
        LocalDate.parse("2026-11-02"),
        TimeOfDay.NOON);
  }
}
