package me.nghlong3004.longnest.ledger.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.AttributeOverrides;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.InterpretationResult;
import me.nghlong3004.longnest.ledger.dto.TransactionPatch;
import me.nghlong3004.longnest.ledger.model.ControlProof;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.model.entity.ControlEvent;
import me.nghlong3004.longnest.ledger.model.entity.CreateInbox;
import me.nghlong3004.longnest.ledger.model.entity.EntryValues;
import me.nghlong3004.longnest.ledger.model.entity.InboxId;
import me.nghlong3004.longnest.ledger.model.entity.LedgerControl;
import me.nghlong3004.longnest.ledger.model.entity.LedgerEntry;
import me.nghlong3004.longnest.ledger.model.entity.OwnerHead;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Control;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlKind;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlStatus;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Entry;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Constructor/guard audit across model categories; provider runtime is separately unverified. */
class LedgerModelAuditTests {
  private static final Instant NOW = Instant.parse("2026-10-21T04:00:00Z");

  private static Entry entry() {
    return new Entry("personal", 42, 1,
        new CreateIntent(new TransactionDraft(TransactionType.EXPENSE, new Money(45000),
            Category.FOOD, new Note("ăn trưa")), LocalDate.parse("2026-10-20")),
        NOW, Optional.of(NOW), false);
  }

  @Test
  void privateBuilderKeepsJpaNoargAndCannotExposeRawControlState() throws Exception {
    var entry = entry();
    var control = new Control("a".repeat(32), "personal", "tg:123:77", "event-1",
        ControlKind.UPDATE, ControlStatus.PENDING, null, null, null,
        entry, entry, NOW.plusSeconds(600), null, null, null, "b".repeat(64));
    var entity = LedgerControl.frozen(control);
    assertThat(entity.snapshot()).isEqualTo(control);
    assertThat(ReflectionTestUtils.getField(entity, "rowVersion")).isNull();
    assertThat(LedgerControl.class.getDeclaredConstructor().getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isProtected(modifiers)).isTrue());
    assertThat(LedgerControl.class.getDeclaredConstructors())
        .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers()));
    assertThat(LedgerControl.class.getDeclaredMethod("builder").getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isPrivate(modifiers)).isTrue());
    assertThatThrownBy(() -> LedgerControl.frozen(new Control(control.id(), control.owner(),
        control.scope(), control.requestId(), control.kind(), ControlStatus.COMPLETED,
        null, null, null, entry, entry, control.expiresAt(), null, 1L, true,
        control.requestFingerprint()))).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void eventBindingUsesGuardedFactoryAndPrivateRawConstructor() throws Exception {
    var bound = ControlEvent.bound("personal", "callback-1", "a".repeat(64), "b".repeat(32));
    assertThat(bound.fingerprint()).isEqualTo("a".repeat(64));
    assertThat(ControlEvent.class.getDeclaredConstructor().getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isProtected(modifiers)).isTrue());
    assertThat(ControlEvent.class.getDeclaredConstructors())
        .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers()));
    assertThatThrownBy(() -> ControlEvent.bound("personal", "callback-1", "bad", "b".repeat(32)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void snapshotColumnOverridesMatchV2FinancialStringLengths() throws Exception {
    for (String field : new String[] {"beforeValues", "afterValues"}) {
      var overrides = LedgerControl.class.getDeclaredField(field)
          .getAnnotation(AttributeOverrides.class);
      assertThat(overrides).isNotNull();
      assertThat(overrides.value()).filteredOn(override -> override.name().equals("note"))
          .singleElement().satisfies(override ->
              assertThat(override.column().length()).isEqualTo(1024));
      for (var override : overrides.value()) {
        if (override.name().equals("type")) assertThat(override.column().length()).isEqualTo(16);
        if (override.name().equals("category")) assertThat(override.column().length()).isEqualTo(32);
      }
    }
  }

  @Test
  void additiveMigrationRetainsBoundedSnapshotsAndGlobalEventIdentity() throws Exception {
    try (var stream = getClass().getResourceAsStream("/db/ledger/V2__ledger_controls.sql")) {
      assertThat(stream).isNotNull();
      String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(sql).contains("CREATE TABLE ledger_control (",
          "UNIQUE (owner_id, request_id)", "CREATE TABLE ledger_control_event (",
          "PRIMARY KEY (owner_id, operation_id)",
          "before_amount BETWEEN 1 AND 1000000000000",
          "after_amount BETWEEN 1 AND 1000000000000",
          "receipt_generation >= 0", "display_message > 0",
          "kind = 'PROMPT' AND field IS NOT NULL",
          "before_date BETWEEN DATE '1900-01-01' AND DATE '9999-12-31'",
          "after_date BETWEEN DATE '1900-01-01' AND DATE '9999-12-31'");
    }
  }

  @Test
  void otherModelCategoriesRetainValidationRecordsAndControlledConstructors() throws Exception {
    assertThat(Money.class.isRecord()).isTrue();
    assertThat(Note.class.isRecord()).isTrue();
    assertThat(TransactionDraft.class.isRecord()).isTrue();
    assertThat(InterpretationResult.Ready.class.isRecord()).isTrue();
    assertThat(HostContext.class.isRecord()).isTrue();
    assertThat(TransactionPatch.class.isRecord()).isTrue();
    assertThatThrownBy(() -> new Money(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Note("a\nb")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PersonalOwner(" ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TelegramSettings("bad", 77))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TransactionPatch(null, Optional.empty(), Optional.empty(),
        Optional.empty(), Optional.empty())).isInstanceOf(IllegalArgumentException.class);
    assertThat(OwnerHead.class.getDeclaredConstructor().getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isProtected(modifiers)).isTrue());
    assertThat(LedgerEntry.class.getDeclaredConstructor().getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isProtected(modifiers)).isTrue());
    assertThat(CreateInbox.class.getDeclaredConstructor().getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isProtected(modifiers)).isTrue());
    assertThat(EntryValues.class.getDeclaredConstructor().getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isProtected(modifiers)).isTrue());
    assertThat(InboxId.class.getDeclaredConstructor().getModifiers())
        .satisfies(modifiers -> assertThat(Modifier.isProtected(modifiers)).isTrue());
    assertThat(ControlProof.class.getDeclaredConstructors())
        .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers()));
    var proof = new ControlProofs().issue("tg:123:77", 101, "callback", "a".repeat(32), 42, 1);
    assertThat(proof.scope()).isEqualTo("tg:123:77");
    assertThat(proof.messageId()).isEqualTo(101);
    assertThat(proof.callbackId()).isEqualTo("callback");
    assertThat(ControlProof.class.getMethods()).extracting(method -> method.getName())
        .doesNotContain("getSignature", "getScope", "getCallbackId");
    assertThat(ControlEvent.class.getMethods()).extracting(method -> method.getName())
        .doesNotContain("getFingerprint", "getControlId");
    var constructor = ControlProof.class.getDeclaredConstructor(String.class, int.class,
        String.class, byte[].class);
    constructor.setAccessible(true);
    byte[] source = {1, 2, 3};
    var copied = constructor.newInstance("tg:123:77", 101, "callback", source);
    source[0] = 9;
    var signature = ControlProof.class.getDeclaredMethod("signature");
    signature.setAccessible(true);
    byte[] read = (byte[]) signature.invoke(copied);
    assertThat(read).containsExactly(1, 2, 3);
    read[0] = 8;
    assertThat((byte[]) signature.invoke(copied)).containsExactly(1, 2, 3);
    assertThat(proof.toString()).doesNotContain("callback", "tg:123:77");
  }
}
