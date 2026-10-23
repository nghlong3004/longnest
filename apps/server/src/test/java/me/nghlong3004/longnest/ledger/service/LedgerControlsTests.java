package me.nghlong3004.longnest.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.ledger.dto.TransactionPatch;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.mapper.TransactionRepresentationMapper;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Control;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlKind;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlStatus;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Entry;
import me.nghlong3004.longnest.ledger.service.impl.TransactionServiceImpl;
import me.nghlong3004.longnest.presentation.UiMessagesTests;
import me.nghlong3004.longnest.telegram.adapter.TelegramCapture;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Synthetic port state only: exercises frozen controls, not a PostgreSQL transaction. */
class LedgerControlsTests {
  private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
  private static final Instant NOW = Instant.parse("2026-10-21T04:00:00Z");
  private static final String SCOPE = "tg:123:77";
  private final LedgerStore store = mock(LedgerStore.class);
  private final Map<String, Control> controls = new HashMap<>();
  private final Map<String, String> callbackEvents = new HashMap<>();
  private final AtomicLong generation = new AtomicLong();
  private final AtomicReference<Entry> entry = new AtomicReference<>();
  private final ControlProofs proofs = new ControlProofs();
  private final TransactionMapper mapper = new TransactionMapper(VALIDATORS.getValidator(),
      Mappers.getMapper(TransactionRepresentationMapper.class));
  private final TransactionServiceImpl service = new TransactionServiceImpl(
      new PersonalOwner("personal"), store, mapper, Clock.fixed(NOW, ZoneOffset.UTC), proofs);

  @AfterAll
  static void closeValidator() { VALIDATORS.close(); }

  private static HostContext host(String operation) {
    return new HostContext("personal", operation, NOW, NOW, SCOPE);
  }

  private static Entry original() {
    return new Entry("personal", 42, 1,
        new CreateIntent(new TransactionDraft(TransactionType.EXPENSE, new Money(45000),
            Category.FOOD, new Note("ăn trưa")), LocalDate.parse("2026-10-20")),
        NOW, Optional.of(NOW), false);
  }

  @BeforeEach
  void setup() {
    entry.set(original());
    when(store.inbox(any(), any())).thenReturn(Optional.empty());
    when(store.lockHead("personal")).thenAnswer(call -> generation.get());
    when(store.entry(eq("personal"), eq(42L))).thenAnswer(call -> Optional.of(entry.get()));
    when(store.control(any(), any())).thenAnswer(call -> Optional.ofNullable(
        controls.get(call.getArgument(1))));
    when(store.controlByRequest(any(), any())).thenAnswer(call -> controls.values().stream()
        .filter(control -> control.owner().equals(call.getArgument(0))
            && control.requestId().equals(call.getArgument(1))).findFirst());
    when(store.controlEventFingerprint(any(), any())).thenAnswer(call -> Optional.ofNullable(
        callbackEvents.get(call.getArgument(1))));
    doAnswer(call -> { callbackEvents.put(call.getArgument(1), call.getArgument(2));
      return null; }).when(store).bindControlEvent(any(), any(), any(), any());
    when(store.promptForReply(any(), any(), anyLong())).thenAnswer(call -> controls.values().stream()
        .filter(control -> control.owner().equals(call.getArgument(0))
            && control.scope().equals(call.getArgument(1))
            && control.kind() == ControlKind.PROMPT && control.displayMessage() != null
            && control.displayMessage() == (long) call.getArgument(2)).findFirst());
    when(store.liveControls(any(), any())).thenAnswer(call -> controls.values().stream()
        .filter(control -> control.status() == ControlStatus.PENDING
            && control.expiresAt().isAfter(call.getArgument(1))).count());
    doAnswer(call -> { Control control = call.getArgument(0); controls.put(control.id(), control);
      return null; }).when(store).addControl(any());
    doAnswer(call -> {
      String id = call.getArgument(1);
      Control old = controls.get(id);
      controls.put(id, copy(old, old.status(), old.successorId(), old.generation(), old.changed(),
          call.getArgument(2)));
      return null;
    }).when(store).bindControl(any(), any(), anyLong());
    doAnswer(call -> {
      String id = call.getArgument(1);
      Control old = controls.get(id);
      controls.put(id, copy(old, call.getArgument(2), call.getArgument(3),
          call.getArgument(4), call.getArgument(5), old.displayMessage()));
      return null;
    }).when(store).finishControl(any(), any(), any(), any(), any(), any());
    doAnswer(call -> {
      controls.replaceAll((id, old) -> old.kind() != ControlKind.PROMPT
          && old.status() == ControlStatus.PENDING && old.before().id() == (long) call.getArgument(2)
          ? copy(old, ControlStatus.REPLACED, null, null, null, old.displayMessage()) : old);
      return null;
    }).when(store).replacePendingActions(any(), any(), anyLong());
    when(store.mutate(any(), anyLong(), anyLong(), any(), anyBoolean())).thenAnswer(call -> {
      Entry before = entry.get();
      assertThat(before.revision()).isEqualTo((long) call.getArgument(2));
      Entry after = new Entry(before.owner(), before.id(), before.revision() + 1,
          call.getArgument(3), before.createdAt(), before.messageAt(), call.getArgument(4));
      entry.set(after);
      return after;
    });
    doAnswer(call -> { generation.set(call.getArgument(2)); return null; })
        .when(store).advance(any(), anyLong(), anyLong());
  }

  private static Control copy(Control old, ControlStatus status, String successor,
      Long receiptGeneration, Boolean changed, Long display) {
    return new Control(old.id(), old.owner(), old.scope(), old.requestId(), old.kind(), status,
        old.field(), old.originId(), successor, old.before(), old.after(), old.expiresAt(),
        display, receiptGeneration, changed, old.requestFingerprint());
  }

  private static TransactionPatch patch(String amount, String date) {
    return new TransactionPatch(Optional.ofNullable(amount), Optional.empty(), Optional.empty(),
        Optional.empty(), Optional.ofNullable(date));
  }

  private static void fails(Runnable call, LedgerException.Code code) {
    assertThatThrownBy(call::run).isInstanceOfSatisfying(LedgerException.class,
        error -> assertThat(error.getCode()).isEqualTo(code));
  }

  @Test
  void amountThenDateCarriesUnsavedAfterstateAndOnlyNewestPreviewCanSave() {
    var amountPrompt = service.openFieldPrompt(host("prompt-1"), "42", "1", EditField.AMOUNT, null);
    service.bindPrompt(host("prompt-1"), amountPrompt.promptId(), 101);
    var first = service.previewFieldReply(host("reply-1"), 101, "40k");
    service.bindAction(host("reply-1"), first.actionId(), 102);
    var datePrompt = service.openFieldPrompt(host("prompt-2"), "42", "1", EditField.DATE,
        first.actionId());
    service.bindPrompt(host("prompt-2"), datePrompt.promptId(), 103);
    var second = service.previewFieldReply(host("reply-2"), 103, "2026-10-19");
    assertThat(second.before().amountMinor()).isEqualTo("45000");
    assertThat(second.after().amountMinor()).isEqualTo("40000");
    assertThat(second.after().transactionDate()).isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(controls.get(first.actionId()).status()).isEqualTo(ControlStatus.REPLACED);
    fails(() -> service.update(host("save-old"), first.actionId(),
        proofs.issue(SCOPE, 102, "cb-old", first.actionId(), 42, 1)),
        LedgerException.Code.ACTION_COMPLETED);
    service.bindAction(host("reply-2"), second.actionId(), 104);
    var proof = proofs.issue(SCOPE, 104, "cb-new", second.actionId(), 42, 1);
    var saved = service.update(host("save-new"), second.actionId(), proof);
    assertThat(saved.changed()).isTrue();
    assertThat(saved.transaction().revision()).isEqualTo("2");
    assertThat(saved.transaction().amountMinor()).isEqualTo("40000");
    assertThat(entry.get().intent().transactionDate()).isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(generation.get()).isEqualTo(1);
    assertThat(service.update(host("repeat-button"), second.actionId(), proof).replayed()).isTrue();
    fails(() -> service.previewFieldReply(host("reply-2"), 103, "2026-10-18"),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void directPatchReplayChecksPresenceAndPayloadBeforeLaterTargetRevision() {
    var first = service.previewUpdate(host("same"), "42", "1", patch("40k", null), null);
    service.bindAction(host("same"), first.actionId(), 102);
    service.update(host("save"), first.actionId(),
        proofs.issue(SCOPE, 102, "cb", first.actionId(), 42, 1));
    assertThat(service.previewUpdate(host("same"), "42", "1", patch("40k", null), null)
        .actionId()).isEqualTo(first.actionId());
    fails(() -> service.previewUpdate(host("same"), "42", "1", patch("40k", "2026-10-19"), null),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void typeResetNoteDigitsClearAndStrictDateValidation() {
    var type = service.previewUpdate(host("type"), "42", "1",
        new TransactionPatch(Optional.empty(), Optional.of(TransactionType.INCOME),
            Optional.empty(), Optional.empty(), Optional.empty()), null);
    assertThat(type.after().category()).isEqualTo(Category.UNCATEGORIZED);
    fails(() -> service.previewUpdate(host("bad-category"), "42", "1",
        new TransactionPatch(Optional.empty(), Optional.of(TransactionType.INCOME),
            Optional.of(Category.FOOD), Optional.empty(), Optional.empty()), null),
        LedgerException.Code.INVALID_FIELD);
    var note = service.previewUpdate(host("note"), "42", "1",
        new TransactionPatch(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of("mã 123"), Optional.empty()), null);
    assertThat(note.after().note()).isEqualTo("mã 123");
    var clear = service.previewUpdate(host("clear"), "42", "1",
        new TransactionPatch(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(""), Optional.empty()), null);
    assertThat(clear.after().note()).isEmpty();
    for (String bad : new String[] {"1899-12-31", "2026-02-30", "26-10-19", "9999-12-32"}) {
      fails(() -> service.previewUpdate(host("date-" + bad), "42", "1", patch(null, bad), null),
          LedgerException.Code.INVALID_DATE);
    }
  }

  @Test
  void deleteRequiresBoundProofAndRestoreDirectIsStableNoopOrChange() {
    var delete = service.requestDelete(host("delete"), "42", "1");
    assertThat(entry.get().deleted()).isFalse();
    service.bindAction(host("delete"), delete.actionId(), 120);
    fails(() -> service.confirmDelete(host("confirm"), delete.actionId(),
        proofs.issue(SCOPE, 121, "copied", delete.actionId(), 42, 1)),
        LedgerException.Code.INVALID_ACTION_PROOF);
    var removed = service.confirmDelete(host("confirm"), delete.actionId(),
        proofs.issue(SCOPE, 120, "real", delete.actionId(), 42, 1));
    assertThat(removed.transaction().state()).isEqualTo(TransactionView.State.DELETED);
    assertThat(removed.transaction().revision()).isEqualTo("2");
    assertThat(generation.get()).isEqualTo(1);
    var restored = service.restore(host("restore"), "42", "2");
    assertThat(restored.changed()).isTrue();
    assertThat(restored.transaction().revision()).isEqualTo("3");
    assertThat(service.restore(host("restore"), "42", "2").replayed()).isTrue();
    var active = service.restore(host("active"), "42", "3");
    assertThat(active.changed()).isFalse();
    assertThat(active.transaction().revision()).isEqualTo("3");
    assertThat(generation.get()).isEqualTo(2);
  }

  @Test
  void invalidReplyKeepsPromptPendingAndCancelIsIdempotent() {
    var prompt = service.openFieldPrompt(host("prompt"), "42", "1", EditField.AMOUNT, null);
    service.bindPrompt(host("prompt"), prompt.promptId(), 101);
    fails(() -> service.previewFieldReply(host("reply-bad"), 101, "-40k"),
        LedgerException.Code.INVALID_AMOUNT);
    assertThat(controls.get(prompt.promptId()).status()).isEqualTo(ControlStatus.PENDING);
    var proof = proofs.issue(SCOPE, 101, "cb", prompt.promptId(), 42, 1);
    service.cancel(host("cancel"), prompt.promptId(), proof);
    service.cancel(host("cancel-again"), prompt.promptId(), proof);
    fails(() -> service.previewFieldReply(host("reply"), 101, "40k"),
        LedgerException.Code.ACTION_CANCELED);
    assertThat(entry.get()).isEqualTo(original());
  }

  @Test
  void unauthorizedIsRejectedBeforeStoreInspection() {
    fails(() -> service.requestDelete(new HostContext("foreign", "x", NOW, NOW, SCOPE),
        "42", "1"), LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
  }

  @Test
  void hostOperationCannotChangeScopeKindOrCrossCreateInbox() {
    service.previewUpdate(host("same"), "42", "1", patch("40k", null), null);
    var otherScope = new HostContext("personal", "same", NOW, NOW, "tg:123:other");
    fails(() -> service.previewUpdate(otherScope, "42", "1", patch("40k", null), null),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
    fails(() -> service.requestDelete(host("same"), "42", "1"),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
    var input = new TransactionInput(TransactionType.EXPENSE, "45000", "VND",
        Optional.empty(), Optional.of(Category.FOOD), Optional.of("ăn trưa"));
    fails(() -> service.ingest(host("same"), input),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
    var intent = original().intent();
    when(store.inbox("personal", "purchase")).thenReturn(Optional.of(new LedgerStore.Inbox(
        "personal", "purchase", mapper.fingerprint(intent), intent, NOW, Optional.of(NOW),
        Optional.empty())));
    fails(() -> service.requestDelete(host("purchase"), "42", "1"),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void oneCallbackEventCannotSwitchActionButDistinctClicksReplayOriginal() {
    var first = service.previewUpdate(host("prepare-update"), "42", "1",
        patch("40k", null), null);
    service.bindAction(host("prepare-update"), first.actionId(), 111);
    var firstProof = proofs.issue(SCOPE, 111, "callback-a", first.actionId(), 42, 1);
    var saved = service.update(host("same-callback-event"), first.actionId(), firstProof);
    assertThat(service.update(host("different-callback-event"), first.actionId(),
        proofs.issue(SCOPE, 111, "callback-b", first.actionId(), 42, 1)).replayed()).isTrue();
    assertThat(saved.generation()).isEqualTo("1");
    var delete = service.requestDelete(host("prepare-delete"), "42", "2");
    service.bindAction(host("prepare-delete"), delete.actionId(), 112);
    fails(() -> service.confirmDelete(host("same-callback-event"), delete.actionId(),
        proofs.issue(SCOPE, 112, "callback-c", delete.actionId(), 42, 2)),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
    assertThat(entry.get().deleted()).isFalse();
    fails(() -> service.cancel(host("same-callback-event"), delete.actionId(),
        proofs.issue(SCOPE, 112, "callback-d", delete.actionId(), 42, 2)),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void noopUpdateCompletesWithoutRevisionOrGenerationAndLaterCancelIsCompleted() {
    var preview = service.previewUpdate(host("noop"), "42", "1", patch("45000", null), null);
    service.bindAction(host("noop"), preview.actionId(), 130);
    var proof = proofs.issue(SCOPE, 130, "noop-click", preview.actionId(), 42, 1);
    var saved = service.update(host("noop-save"), preview.actionId(), proof);
    assertThat(saved.changed()).isFalse();
    assertThat(saved.transaction().revision()).isEqualTo("1");
    assertThat(saved.generation()).isEqualTo("0");
    assertThat(generation.get()).isZero();
    assertThat(entry.get()).isEqualTo(original());
    fails(() -> service.cancel(host("late-cancel"), preview.actionId(), proof),
        LedgerException.Code.ACTION_COMPLETED);
  }

  @Test
  void expirationAndStaleTargetNeverMutateEvenWithValidProof() {
    var preview = service.previewUpdate(host("expire"), "42", "1", patch("40k", null), null);
    service.bindAction(host("expire"), preview.actionId(), 130);
    var proof = proofs.issue(SCOPE, 130, "expired-click", preview.actionId(), 42, 1);
    var later = new TransactionServiceImpl(new PersonalOwner("personal"), store, mapper,
        Clock.fixed(NOW.plusSeconds(601), ZoneOffset.UTC), proofs);
    fails(() -> later.update(host("expire-save"), preview.actionId(), proof),
        LedgerException.Code.ACTION_EXPIRED);
    assertThat(generation.get()).isZero();
    var newer = service.previewUpdate(host("stale"), "42", "1", patch("40000", null), null);
    service.bindAction(host("stale"), newer.actionId(), 131);
    entry.set(new Entry("personal", 42, 2, original().intent(), NOW, Optional.of(NOW), false));
    fails(() -> service.update(host("stale-save"), newer.actionId(),
        proofs.issue(SCOPE, 131, "stale-click", newer.actionId(), 42, 1)),
        LedgerException.Code.REVISION_CONFLICT);
    assertThat(generation.get()).isZero();
  }

  @Test
  void deleteIsOnlyPreviewUntilConfirmAndCompletedReceiptStaysOriginalAfterRestore() {
    var preview = service.requestDelete(host("delete"), "42", "1");
    assertThat(preview.before().state()).isEqualTo(TransactionView.State.ACTIVE);
    assertThat(preview.after().state()).isEqualTo(TransactionView.State.DELETED);
    assertThat(entry.get().deleted()).isFalse();
    service.bindAction(host("delete"), preview.actionId(), 140);
    var proof = proofs.issue(SCOPE, 140, "confirm", preview.actionId(), 42, 1);
    var deleted = service.confirmDelete(host("delete-click"), preview.actionId(), proof);
    service.restore(host("restore-click"), "42", "2");
    var replay = service.confirmDelete(host("delete-click-again"), preview.actionId(), proof);
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.transaction()).isEqualTo(deleted.transaction());
    assertThat(entry.get().deleted()).isFalse();
    assertThat(generation.get()).isEqualTo(2);
    fails(() -> service.requestDelete(host("old-revision"), "42", "1"),
        LedgerException.Code.REVISION_CONFLICT);
  }

  @Test
  void deleteCommandFreezesRevisionBeforeRepeatCurrentLookup() {
    var first = service.requestDeleteCurrent(host("delete-command"), "42");
    assertThat(first.before().revision()).isEqualTo("1");
    service.bindAction(host("delete-command"), first.actionId(), 170);
    service.confirmDelete(host("delete-confirm"), first.actionId(),
        proofs.issue(SCOPE, 170, "delete-click", first.actionId(), 42, 1));
    var replay = service.requestDeleteCurrent(host("delete-command"), "42");
    assertThat(replay.actionId()).isEqualTo(first.actionId());
    assertThat(replay.before()).isEqualTo(first.before());
    assertThat(replay.after()).isEqualTo(first.after());
    assertThat(entry.get().deleted()).isTrue();
    assertThat(entry.get().revision()).isEqualTo(2);
    assertThat(generation.get()).isEqualTo(1);
    fails(() -> service.requestDeleteCurrent(host("delete-command"), "43"),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
    fails(() -> service.requestDelete(host("delete-command"), "42", "2"),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void callbackMutationsRejectMissingOrOversizedHostOperationBeforeStoreLookup() {
    var update = service.previewUpdate(host("prepare"), "42", "1", patch("40k", null), null);
    service.bindAction(host("prepare"), update.actionId(), 151);
    var proof = proofs.issue(SCOPE, 151, "callback", update.actionId(), 42, 1);
    var delete = service.requestDelete(host("prepare-delete"), "42", "1");
    service.bindAction(host("prepare-delete"), delete.actionId(), 152);
    var deleteProof = proofs.issue(SCOPE, 152, "callback", delete.actionId(), 42, 1);
    for (String operation : new String[] {null, "", " ", "x".repeat(201)}) {
      var invalid = new HostContext("personal", operation, NOW, NOW, SCOPE);
      fails(() -> service.update(invalid, update.actionId(), proof),
          LedgerException.Code.INVALID_FIELD);
      fails(() -> service.cancel(invalid, update.actionId(), proof),
          LedgerException.Code.INVALID_FIELD);
      fails(() -> service.confirmDelete(invalid, delete.actionId(), deleteProof),
          LedgerException.Code.INVALID_FIELD);
      fails(() -> service.bindAction(invalid, delete.actionId(), 152),
          LedgerException.Code.INVALID_FIELD);
    }
    assertThat(entry.get()).isEqualTo(original());
    assertThat(generation.get()).isZero();
  }

  @Test
  void capAndWrongProofFailClosedWithoutMutation() {
    when(store.liveControls(eq("personal"), any())).thenReturn(100L);
    fails(() -> service.requestDelete(host("at-cap"), "42", "1"),
        LedgerException.Code.LIMIT_REACHED);
    when(store.liveControls(eq("personal"), any())).thenReturn(0L);
    var preview = service.requestDelete(host("delete"), "42", "1");
    service.bindAction(host("delete"), preview.actionId(), 140);
    fails(() -> service.confirmDelete(host("commit"), preview.actionId(), null),
        LedgerException.Code.INVALID_ACTION_PROOF);
    fails(() -> service.confirmDelete(new HostContext("foreign", "commit", NOW, NOW, SCOPE),
        preview.actionId(), proofs.issue(SCOPE, 140, "cb", preview.actionId(), 42, 1)),
        LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    assertThat(entry.get().deleted()).isFalse();
    assertThat(generation.get()).isZero();
  }

  @Test
  void incomeSupportsTheSameEditDeleteRestoreLifecycle() {
    entry.set(new Entry("personal", 42, 1,
        new CreateIntent(new TransactionDraft(TransactionType.INCOME, new Money(25_000_000),
            Category.SALARY, new Note("lương 10")), LocalDate.parse("2026-10-20")),
        NOW, Optional.of(NOW), false));
    var preview = service.previewUpdate(host("income-edit"), "42", "1",
        new TransactionPatch(Optional.of("24000000"), Optional.empty(), Optional.empty(),
            Optional.of("lương 10 sửa"), Optional.of("2026-10-19")), null);
    assertThat(preview.before().type()).isEqualTo(TransactionType.INCOME);
    assertThat(preview.after().amountMinor()).isEqualTo("24000000");
    service.bindAction(host("income-edit"), preview.actionId(), 150);
    service.update(host("income-save"), preview.actionId(),
        proofs.issue(SCOPE, 150, "income-cb", preview.actionId(), 42, 1));
    var delete = service.requestDelete(host("income-delete"), "42", "2");
    service.bindAction(host("income-delete"), delete.actionId(), 151);
    service.confirmDelete(host("income-confirm"), delete.actionId(),
        proofs.issue(SCOPE, 151, "income-del", delete.actionId(), 42, 2));
    var restored = service.restore(host("income-restore"), "42", "3");
    assertThat(restored.transaction().type()).isEqualTo(TransactionType.INCOME);
    assertThat(restored.transaction().amountMinor()).isEqualTo("24000000");
    assertThat(restored.transaction().transactionDate())
        .isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(restored.transaction().revision()).isEqualTo("4");
    assertThat(generation.get()).isEqualTo(3);
  }

  @Test
  void displayedRestoreButtonReplaysOriginalOutcomeAcrossSdkDeliveryIds() throws Exception {
    var queries = mock(LedgerQueryService.class);
    when(queries.get(any(), eq("42"))).thenAnswer(call -> mapper.view(entry.get()));
    var client = mock(TelegramClient.class);
    var sends = new ArrayList<SendMessage>();
    var delivered = new ArrayList<Message>();
    when(client.execute(any(AnswerCallbackQuery.class))).thenReturn(true);
    when(client.execute(any(SendMessage.class))).thenAnswer(call -> {
      var request = (SendMessage) call.getArgument(0);
      sends.add(request);
      var response = new Message();
      response.setMessageId(100 + sends.size());
      response.setChat(new Chat(77L, "private"));
      delivered.add(response);
      return response;
    });
    var capture = new TelegramCapture(client, new TelegramSettings("123:" + "x".repeat(30), 77),
        new PersonalOwner("personal"), new TransactionInterpreter(), mapper,
        new LedgerClient(new PersonalOwner("personal"), service, queries),
        new TelegramReplies(UiMessagesTests.vietnamese()), Clock.fixed(NOW, ZoneOffset.UTC),
        new TelegramNavigation(Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom()), proofs);
    var command = new Message();
    command.setMessageId(1);
    command.setFrom(new User(77L, "owner", false));
    command.setChat(new Chat(77L, "private"));
    command.setText("/show 42");
    command.setDate(Math.toIntExact(NOW.getEpochSecond()));
    var show = new Update();
    show.setUpdateId(1);
    show.setMessage(command);
    assertThat(capture.handle(show)).isTrue();
    String delete = button(sends.getLast(), "Xóa");
    assertThat(capture.handle(callback(delivered.getLast(), delete, 2))).isTrue();
    assertThat(entry.get().deleted()).isFalse();
    String confirm = button(sends.getLast(), "Xác nhận xóa");
    assertThat(capture.handle(callback(delivered.getLast(), confirm, 3))).isTrue();
    assertThat(entry.get().deleted()).isTrue();
    Message displayed = delivered.getLast();
    String restore = button(sends.getLast(), "Khôi phục");
    assertThat(restore).matches("a:[0-9a-f]{32}:r");
    var foreign = callback(displayed, restore, 30);
    foreign.getCallbackQuery().getFrom().setId(88L);
    assertThat(capture.handle(foreign)).isTrue();
    var wrongChat = new Message();
    wrongChat.setMessageId(displayed.getMessageId());
    wrongChat.setChat(new Chat(88L, "private"));
    assertThat(capture.handle(callback(wrongChat, restore, 31))).isTrue();
    var copied = new Message();
    copied.setMessageId(999);
    copied.setChat(new Chat(77L, "private"));
    assertThat(capture.handle(callback(copied, restore, 32))).isTrue();
    assertThat(capture.handle(callback(displayed, "a:" + "f".repeat(32) + ":r", 33))).isTrue();
    assertThat(entry.get().deleted()).isTrue();
    assertThat(capture.handle(callback(displayed, restore, 4))).isTrue();
    assertThat(capture.handle(callback(displayed, restore, 5))).isTrue();
    assertThat(entry.get().revision()).isEqualTo(3);
    assertThat(generation.get()).isEqualTo(2);
    assertThat(sends.getLast().getText()).contains("Kết quả đã lưu ban đầu", "Phiên bản 3");

    var later = service.previewUpdate(host("later-preview"), "42", "3", patch("40k", null), null);
    service.bindAction(host("later-preview"), later.actionId(), 200);
    fails(() -> service.update(host("tg:123:4"), later.actionId(),
        proofs.issue(SCOPE, 200, "changed-action", later.actionId(), 42, 3)),
        LedgerException.Code.IDEMPOTENCY_CONFLICT);
    service.update(host("later-save"), later.actionId(),
        proofs.issue(SCOPE, 200, "later-click", later.actionId(), 42, 3));
    assertThat(entry.get().revision()).isEqualTo(4);
    var restartedProofs = new ControlProofs();
    var restartedService = new TransactionServiceImpl(new PersonalOwner("personal"), store,
        mapper, Clock.fixed(NOW, ZoneOffset.UTC), restartedProofs);
    capture = new TelegramCapture(client, new TelegramSettings("123:" + "x".repeat(30), 77),
        new PersonalOwner("personal"), new TransactionInterpreter(), mapper,
        new LedgerClient(new PersonalOwner("personal"), restartedService, queries),
        new TelegramReplies(UiMessagesTests.vietnamese()), Clock.fixed(NOW, ZoneOffset.UTC),
        new TelegramNavigation(Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom()),
        restartedProofs);
    assertThat(capture.handle(callback(displayed, restore, 6))).isTrue();
    assertThat(sends.getLast().getText()).contains("Kết quả đã lưu ban đầu", "Phiên bản 3",
        "45000").doesNotContain("40000");
    assertThat(entry.get().revision()).isEqualTo(4);
    assertThat(generation.get()).isEqualTo(3);
  }

  private static String button(SendMessage sent, String label) {
    return ((InlineKeyboardMarkup) sent.getReplyMarkup()).getKeyboard().stream()
        .flatMap(List::stream).filter(button -> button.getText().equals(label))
        .findFirst().orElseThrow().getCallbackData();
  }

  private static Update callback(Message displayed, String data, int delivery) {
    var query = new CallbackQuery();
    query.setId("callback-" + delivery);
    query.setFrom(new User(77L, "owner", false));
    query.setMessage(displayed);
    query.setData(data);
    var update = new Update();
    update.setUpdateId(delivery);
    update.setCallbackQuery(query);
    return update;
  }
}
