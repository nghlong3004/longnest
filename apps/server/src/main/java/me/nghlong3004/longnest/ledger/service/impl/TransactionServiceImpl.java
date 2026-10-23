package me.nghlong3004.longnest.ledger.service.impl;

import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.IDEMPOTENCY_CONFLICT;
import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.LIMIT_REACHED;
import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.NOT_SAVED;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.ControlBinding;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionPatch;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.ControlProof;
import me.nghlong3004.longnest.ledger.model.ControlProofs;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Control;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlKind;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.ControlStatus;
import me.nghlong3004.longnest.ledger.repository.LedgerStore.Entry;
import me.nghlong3004.longnest.ledger.service.TransactionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("ledger")
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED,
    rollbackFor = Exception.class, timeout = 10)
public class TransactionServiceImpl implements TransactionService {
  private final PersonalOwner owner;
  private final LedgerStore store;
  private final TransactionMapper mapper;
  private final Clock clock;
  private final ControlProofs proofs;
  private final SecureRandom random = new SecureRandom();
  private static final Duration CONTROL_TTL = Duration.ofMinutes(10);
  private static final int CONTROL_LIMIT = 100;

  @Override
  public void initializeOwner(HostContext context) {
    String principal = owner.authorize(context);
    if (context.operationId() != null || context.businessReference() != null
        || context.messageAt() != null) {
      throw new LedgerException(LedgerException.Code.INVALID_FIELD);
    }
    store.initializeOwner(principal);
  }

  @Override
  public List<String> pendingCreates(HostContext context, String operationPrefix) {
    String principal = owner.authorize(context);
    if (operationPrefix == null || !operationPrefix.matches("tg:[1-9][0-9]{0,19}:")) {
      throw new LedgerException(LedgerException.Code.INVALID_FIELD);
    }
    store.lockHead(principal);
    return List.copyOf(store.pendingCreates(principal, operationPrefix));
  }

  @Override
  public void ingest(HostContext context, TransactionInput input) {
    String principal = owner.authorize(context);
    mapper.validateHost(context);
    store.lockHead(principal);
    if (store.controlByRequest(principal, context.operationId()).isPresent()
        || store.controlEventFingerprint(principal, context.operationId()).isPresent()) {
      throw conflict();
    }
    var existing = store.inbox(principal, context.operationId());
    var reference = existing.map(LedgerStore.Inbox::businessReference)
        .orElseGet(() -> TransactionMapper.micros(context.businessReference()));
    var intent = mapper.normalize(input, reference);
    String fingerprint = mapper.fingerprint(intent);
    if (existing.isPresent()) {
      if (!existing.orElseThrow().fingerprint().equals(fingerprint)) {
        throw new LedgerException(IDEMPOTENCY_CONFLICT);
      }
      return;
    }
    store.freeze(new LedgerStore.Inbox(principal, context.operationId(), fingerprint, intent,
        reference, Optional.ofNullable(context.messageAt()).map(TransactionMapper::micros),
        Optional.empty()));
  }

  @Override
  public MutationReceipt create(HostContext context) {
    String principal = owner.authorize(context);
    mapper.validateOperation(context);
    long generation = store.lockHead(principal);
    if (store.controlByRequest(principal, context.operationId()).isPresent()
        || store.controlEventFingerprint(principal, context.operationId()).isPresent()) {
      throw conflict();
    }
    var inbox = store.inbox(principal, context.operationId())
        .orElseThrow(() -> new LedgerException(NOT_SAVED));
    if (inbox.receipt().isPresent()) {
      return mapper.receipt(inbox, true);
    }
    if (generation == Long.MAX_VALUE) {
      throw new LedgerException(LIMIT_REACHED);
    }
    var entry = store.insert(principal, inbox.intent(), TransactionMapper.micros(clock.instant()),
        inbox.messageAt());
    long next = generation + 1;
    store.advance(principal, generation, next);
    var receipt = new LedgerStore.Receipt(entry.id(), next, entry.createdAt());
    store.complete(principal, context.operationId(), receipt);
    var completed = new LedgerStore.Inbox(inbox.owner(), inbox.operationId(), inbox.fingerprint(),
        inbox.intent(), inbox.businessReference(), inbox.messageAt(), Optional.of(receipt));
    return mapper.receipt(completed, false);
  }

  @Override
  public FieldPrompt openFieldPrompt(HostContext context, String id, String revision,
      EditField field, String previousActionId) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    if (field == null) throw invalid();
    String requestFingerprint = fingerprint("prompt", id, revision, field.name(),
        previousActionId);
    store.lockHead(principal);
    rejectCreateOrCallbackIdentity(principal, context.operationId());
    var repeated = store.controlByRequest(principal, context.operationId());
    if (repeated.isPresent()) {
      var prior = repeated.orElseThrow();
      if (!prior.scope().equals(context.interactionScope())
          || prior.kind() != ControlKind.PROMPT || prior.field() != field
          || prior.before().id() != parseId(id) || prior.before().revision() != parseId(revision)
          || !Objects.equals(prior.originId(), previousActionId)
          || !prior.requestFingerprint().equals(requestFingerprint)) throw conflict();
      return fieldPrompt(prior);
    }
    Entry target = current(principal, id, revision);
    if (target.deleted()) throw new LedgerException(LedgerException.Code.INVALID_STATE);
    Entry proposed = target;
    if (previousActionId != null) {
      Control source = control(principal, previousActionId);
      requireSource(source, context, target);
      proposed = source.after();
    }
    limit(principal);
    var prompt = new Control(newId(), principal, context.interactionScope(),
        context.operationId(), ControlKind.PROMPT, ControlStatus.PENDING, field,
        previousActionId, null, target, proposed, expiry(), null, null, null,
        requestFingerprint);
    store.addControl(prompt);
    return fieldPrompt(prompt);
  }

  @Override
  public void bindPrompt(HostContext context, String promptId, long displayMessage) {
    bind(context, promptId, displayMessage, ControlKind.PROMPT);
  }

  @Override
  public ConfirmationRequired previewFieldReply(HostContext context, long replyToMessage,
      String value) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    if (replyToMessage <= 0) throw invalid();
    store.lockHead(principal);
    rejectCreateOrCallbackIdentity(principal, context.operationId());
    Control prompt = store.promptForReply(principal, context.interactionScope(), replyToMessage)
        .orElseThrow(() -> new LedgerException(LedgerException.Code.ACTION_EXPIRED));
    String requestFingerprint = fingerprint("reply", prompt.id(), value);
    if (prompt.status() == ControlStatus.FILLED && prompt.successorId() != null) {
      Control action = control(principal, prompt.successorId());
      if (!action.requestId().equals(context.operationId())) {
        throw new LedgerException(LedgerException.Code.ACTION_COMPLETED);
      }
      if (!action.requestFingerprint().equals(requestFingerprint)) throw conflict();
      return confirmation(action);
    }
    requirePending(prompt);
    Entry current = current(principal, Long.toString(prompt.before().id()),
        Long.toString(prompt.before().revision()));
    if (current.deleted()) throw new LedgerException(LedgerException.Code.INVALID_STATE);
    TransactionPatch patch = mapper.fieldPatch(prompt.field(), value);
    ConfirmationRequired preview = previewUpdateFrozen(context, Long.toString(current.id()),
        Long.toString(current.revision()), patch, prompt.originId(), requestFingerprint);
    store.finishControl(principal, prompt.id(), ControlStatus.FILLED,
        preview.actionId(), null, null);
    return preview;
  }

  @Override
  public ConfirmationRequired previewUpdate(HostContext context, String id, String revision,
      TransactionPatch patch, String previousActionId) {
    owner.authorize(context);
    return previewUpdateFrozen(context, id, revision, patch, previousActionId,
        patchFingerprint(id, revision, patch, previousActionId));
  }

  private ConfirmationRequired previewUpdateFrozen(HostContext context, String id,
      String revision, TransactionPatch patch, String previousActionId,
      String requestFingerprint) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    store.lockHead(principal);
    rejectCreateOrCallbackIdentity(principal, context.operationId());
    var repeated = store.controlByRequest(principal, context.operationId());
    if (repeated.isPresent()) {
      Control prior = repeated.orElseThrow();
      if (!prior.scope().equals(context.interactionScope())
          || prior.kind() != ControlKind.UPDATE || prior.before().id() != parseId(id)
          || prior.before().revision() != parseId(revision)
          || !Objects.equals(prior.originId(), previousActionId)
          || !prior.requestFingerprint().equals(requestFingerprint)) throw conflict();
      return confirmation(prior);
    }
    Entry target = current(principal, id, revision);
    if (target.deleted()) throw new LedgerException(LedgerException.Code.INVALID_STATE);
    Entry source = target;
    if (previousActionId != null) {
      Control previous = control(principal, previousActionId);
      requireSource(previous, context, target);
      source = previous.after();
    }
    var afterIntent = mapper.patch(source.intent(), patch);
    limit(principal);
    var after = new Entry(principal, target.id(), target.revision(), afterIntent,
        target.createdAt(), target.messageAt(), false);
    store.replacePendingActions(principal, context.interactionScope(), target.id());
    Control action = new Control(newId(), principal, context.interactionScope(),
        context.operationId(), ControlKind.UPDATE, ControlStatus.PENDING, null,
        previousActionId, null, target, after, expiry(), null, null, null,
        requestFingerprint);
    store.addControl(action);
    return confirmation(action);
  }

  @Override
  public ConfirmationRequired requestDelete(HostContext context, String id, String revision) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    String requestFingerprint = fingerprint(ControlKind.DELETE.name(), id, revision);
    store.lockHead(principal);
    rejectCreateOrCallbackIdentity(principal, context.operationId());
    var repeated = store.controlByRequest(principal, context.operationId());
    if (repeated.isPresent()) {
      Control prior = repeated.orElseThrow();
      if (!prior.scope().equals(context.interactionScope())
          || prior.kind() != ControlKind.DELETE || prior.before().id() != parseId(id)
          || prior.before().revision() != parseId(revision)
          || !prior.requestFingerprint().equals(requestFingerprint)) throw conflict();
      return confirmation(prior);
    }
    Entry target = current(principal, id, revision);
    return freezeDelete(context, principal, target, requestFingerprint);
  }

  @Override
  public ConfirmationRequired requestDeleteCurrent(HostContext context, String id) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    String requestFingerprint = fingerprint("deleteCurrent", id);
    store.lockHead(principal);
    rejectCreateOrCallbackIdentity(principal, context.operationId());
    var repeated = store.controlByRequest(principal, context.operationId());
    if (repeated.isPresent()) {
      Control prior = repeated.orElseThrow();
      if (!prior.scope().equals(context.interactionScope())
          || prior.kind() != ControlKind.DELETE || prior.before().id() != parseId(id)
          || !prior.requestFingerprint().equals(requestFingerprint)) throw conflict();
      return confirmation(prior);
    }
    Entry target = store.entry(principal, parseId(id))
        .orElseThrow(() -> new LedgerException(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN));
    if (!principal.equals(target.owner())) {
      throw new LedgerException(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    }
    return freezeDelete(context, principal, target, requestFingerprint);
  }

  private ConfirmationRequired freezeDelete(HostContext context, String principal,
      Entry target, String requestFingerprint) {
    if (target.deleted()) {
      throw new LedgerException(LedgerException.Code.INVALID_STATE);
    }
    limit(principal);
    var after = new Entry(principal, target.id(), target.revision(), target.intent(),
        target.createdAt(), target.messageAt(), true);
    store.replacePendingActions(principal, context.interactionScope(), target.id());
    Control action = new Control(newId(), principal, context.interactionScope(),
        context.operationId(), ControlKind.DELETE, ControlStatus.PENDING, null, null, null,
        target, after, expiry(), null, null, null, requestFingerprint);
    store.addControl(action);
    return confirmation(action);
  }

  @Override
  public ConfirmationRequired openRestoreButton(HostContext context, String id, String revision) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    String requestFingerprint = fingerprint("restoreButton", id, revision);
    store.lockHead(principal);
    rejectCreateOrCallbackIdentity(principal, context.operationId());
    var repeated = store.controlByRequest(principal, context.operationId());
    if (repeated.isPresent()) {
      Control prior = repeated.orElseThrow();
      if (!prior.scope().equals(context.interactionScope())
          || prior.kind() != ControlKind.RESTORE || prior.before().id() != parseId(id)
          || prior.before().revision() != parseId(revision)
          || !prior.requestFingerprint().equals(requestFingerprint)) throw conflict();
      return confirmation(prior);
    }
    Entry target = current(principal, id, revision);
    if (!target.deleted()) throw new LedgerException(LedgerException.Code.INVALID_STATE);
    limit(principal);
    var after = new Entry(principal, target.id(), target.revision(), target.intent(),
        target.createdAt(), target.messageAt(), false);
    Control action = new Control(newId(), principal, context.interactionScope(),
        context.operationId(), ControlKind.RESTORE, ControlStatus.PENDING, null, null,
        null, target, after, expiry(), null, null, null, requestFingerprint);
    store.addControl(action);
    return confirmation(action);
  }

  @Override
  public void bindAction(HostContext context, String actionId, long displayMessage) {
    bind(context, actionId, displayMessage, null);
  }

  @Override
  public ControlBinding controlBinding(HostContext context, String id) {
    String principal = owner.authorize(context);
    requireScope(context);
    store.lockHead(principal);
    Control control = control(principal, id);
    if (!control.scope().equals(context.interactionScope())) throw invalidProof();
    return mapper.controlBinding(control);
  }

  private void bind(HostContext context, String id, long displayMessage, ControlKind expected) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    store.lockHead(principal);
    Control control = control(principal, id);
    if (!control.scope().equals(context.interactionScope())
        || (expected == null ? control.kind() == ControlKind.PROMPT : control.kind() != expected)
        || !control.requestId().equals(context.operationId())) throw invalidProof();
    requirePending(control);
    store.bindControl(principal, id, displayMessage);
  }

  @Override
  public MutationReceipt update(HostContext context, String actionId, ControlProof proof) {
    return consume(context, actionId, proof, ControlKind.UPDATE);
  }

  @Override
  public MutationReceipt confirmDelete(HostContext context, String actionId, ControlProof proof) {
    return consume(context, actionId, proof, ControlKind.DELETE);
  }

  @Override
  public MutationReceipt restoreAction(HostContext context, String actionId, ControlProof proof) {
    return consume(context, actionId, proof, ControlKind.RESTORE);
  }

  @Override
  public MutationReceipt restore(HostContext context, String id, String expectedRevision) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    long generation = store.lockHead(principal);
    String requestFingerprint = fingerprint("restoreDirect", id, expectedRevision);
    rejectCreateOrCallbackIdentity(principal, context.operationId());
    var repeated = store.controlByRequest(principal, context.operationId());
    Control action;
    if (repeated.isPresent()) {
      action = repeated.orElseThrow();
      if (!action.scope().equals(context.interactionScope())
          || action.kind() != ControlKind.RESTORE
          || !action.requestFingerprint().equals(requestFingerprint)) throw conflict();
      if (action.status() == ControlStatus.COMPLETED) return receipt(action, true);
      requirePending(action);
    } else {
      Entry target = current(principal, id, expectedRevision);
      limit(principal);
      var after = new Entry(principal, target.id(), target.revision(), target.intent(),
          target.createdAt(), target.messageAt(), false);
      action = new Control(newId(), principal, context.interactionScope(),
          context.operationId(), ControlKind.RESTORE, ControlStatus.PENDING, null, null,
          null, target, after, expiry(), null, null, null, requestFingerprint);
      store.addControl(action);
    }
    return commitAction(principal, action, generation);
  }

  private MutationReceipt consume(HostContext context, String id, ControlProof proof,
      ControlKind expected) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    long generation = store.lockHead(principal);
    boolean eventKnown = checkCallbackIdentity(principal, context, id, expected.name());
    Control action = control(principal, id);
    if (action.kind() != expected) throw invalidProof();
    verifyProof(action, context, proof);
    if (!eventKnown) bindCallbackIdentity(principal, context, id, expected.name());
    if (action.status() == ControlStatus.COMPLETED) return receipt(action, true);
    requirePending(action);
    return commitAction(principal, action, generation);
  }

  private MutationReceipt commitAction(String principal, Control action, long generation) {
    Entry current = store.entry(principal, action.before().id())
        .orElseThrow(() -> new LedgerException(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN));
    if (current.revision() != action.before().revision()) {
      throw new LedgerException(LedgerException.Code.REVISION_CONFLICT);
    }
    boolean changed = !current.intent().equals(action.after().intent())
        || current.deleted() != action.after().deleted();
    if (changed) {
      if (generation == Long.MAX_VALUE || current.revision() == Long.MAX_VALUE) {
        throw new LedgerException(LIMIT_REACHED);
      }
      store.mutate(principal, current.id(), current.revision(), action.after().intent(),
          action.after().deleted());
      store.advance(principal, generation, generation + 1);
      generation++;
    }
    store.finishControl(principal, action.id(), ControlStatus.COMPLETED, null,
        generation, changed);
    var completed = new Control(action.id(), action.owner(), action.scope(), action.requestId(),
        action.kind(), ControlStatus.COMPLETED, null, action.originId(), null,
        action.before(), action.after(), action.expiresAt(), action.displayMessage(),
        generation, changed, action.requestFingerprint());
    return receipt(completed, false);
  }

  @Override
  public void cancel(HostContext context, String id, ControlProof proof) {
    String principal = owner.authorize(context);
    requireControlContext(context);
    store.lockHead(principal);
    boolean eventKnown = checkCallbackIdentity(principal, context, id, "CANCEL");
    Control control = control(principal, id);
    verifyProof(control, context, proof);
    if (!eventKnown) bindCallbackIdentity(principal, context, id, "CANCEL");
    if (control.status() == ControlStatus.CANCELED) return;
    if (control.status() == ControlStatus.COMPLETED) {
      throw new LedgerException(LedgerException.Code.ACTION_COMPLETED);
    }
    requirePending(control);
    store.finishControl(principal, id, ControlStatus.CANCELED, null, null, null);
  }

  private void verifyProof(Control control, HostContext context, ControlProof proof) {
    if (control.displayMessage() == null || control.displayMessage() <= 0
        || !control.scope().equals(context.interactionScope())
        || control.displayMessage() > Integer.MAX_VALUE
        || !proofs.verifies(proof, control.scope(), control.displayMessage().intValue(),
            control.id(), control.before().id(), control.before().revision())) {
      throw invalidProof();
    }
  }

  private void requirePending(Control control) {
    if (control.status() == ControlStatus.REPLACED
        || control.status() == ControlStatus.FILLED
        || control.status() == ControlStatus.COMPLETED) {
      throw new LedgerException(LedgerException.Code.ACTION_COMPLETED);
    }
    if (control.status() == ControlStatus.CANCELED) {
      throw new LedgerException(LedgerException.Code.ACTION_CANCELED);
    }
    if (!clock.instant().isBefore(control.expiresAt())) {
      throw new LedgerException(LedgerException.Code.ACTION_EXPIRED);
    }
  }

  private void requireSource(Control source, HostContext context, Entry target) {
    if (source.kind() != ControlKind.UPDATE || source.status() != ControlStatus.PENDING
        || !source.scope().equals(context.interactionScope())
        || source.before().id() != target.id()
        || source.before().revision() != target.revision()) throw conflict();
    requirePending(source);
  }

  private Entry current(String principal, String id, String revision) {
    long key = parseId(id);
    long expected = parseId(revision);
    Entry entry = store.entry(principal, key)
        .orElseThrow(() -> new LedgerException(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN));
    if (!principal.equals(entry.owner())) {
      throw new LedgerException(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN);
    }
    if (entry.revision() != expected) {
      throw new LedgerException(LedgerException.Code.REVISION_CONFLICT);
    }
    return entry;
  }

  private void limit(String principal) {
    if (store.liveControls(principal, clock.instant()) >= CONTROL_LIMIT) {
      throw new LedgerException(LIMIT_REACHED);
    }
  }

  private Control control(String principal, String id) {
    return store.control(principal, id)
        .orElseThrow(() -> new LedgerException(LedgerException.Code.NOT_FOUND_OR_FORBIDDEN));
  }

  private FieldPrompt fieldPrompt(Control control) {
    return mapper.fieldPrompt(control);
  }

  private ConfirmationRequired confirmation(Control control) {
    return mapper.confirmation(control);
  }

  private MutationReceipt receipt(Control action, boolean replayed) {
    return mapper.actionReceipt(action, replayed);
  }

  private void requireControlContext(HostContext context) {
    mapper.validateOperation(context);
    requireScope(context);
  }

  private static void requireScope(HostContext context) {
    if (context.interactionScope() == null || context.interactionScope().isBlank()
        || context.interactionScope().length() > 100) throw invalid();
  }

  private Instant expiry() { return TransactionMapper.micros(clock.instant().plus(CONTROL_TTL)); }

  private String newId() {
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private static long parseId(String id) {
    try {
      if (id == null || !id.matches("[1-9][0-9]{0,18}")) throw new NumberFormatException();
      return Long.parseLong(id);
    } catch (NumberFormatException invalid) {
      throw invalid();
    }
  }

  private static LedgerException invalid() {
    return new LedgerException(LedgerException.Code.INVALID_FIELD);
  }

  private static LedgerException conflict() {
    return new LedgerException(IDEMPOTENCY_CONFLICT);
  }

  private static LedgerException invalidProof() {
    return new LedgerException(LedgerException.Code.INVALID_ACTION_PROOF);
  }

  private void rejectCreateOrCallbackIdentity(String principal, String operationId) {
    if (store.inbox(principal, operationId).isPresent()
        || store.controlEventFingerprint(principal, operationId).isPresent()) throw conflict();
  }

  private boolean checkCallbackIdentity(String principal, HostContext context,
      String actionId, String verb) {
    if (store.inbox(principal, context.operationId()).isPresent()
        || store.controlByRequest(principal, context.operationId()).isPresent()) throw conflict();
    var existing = store.controlEventFingerprint(principal, context.operationId());
    if (existing.isPresent() && !existing.orElseThrow().equals(
        fingerprint("callback", context.interactionScope(), actionId, verb))) throw conflict();
    return existing.isPresent();
  }

  private void bindCallbackIdentity(String principal, HostContext context,
      String actionId, String verb) {
    store.bindControlEvent(principal, context.operationId(),
        fingerprint("callback", context.interactionScope(), actionId, verb), actionId);
  }

  private static String patchFingerprint(String id, String revision, TransactionPatch patch,
      String previousActionId) {
    if (patch == null) throw invalid();
    return fingerprint("update", id, revision, previousActionId,
        optional(patch.amountMinor()), optional(patch.type()),
        optional(patch.category()), optional(patch.note()),
        optional(patch.transactionDate()));
  }

  private static String optional(Optional<?> value) {
    if (value == null) throw invalid();
    return value.isPresent() ? "1" + value.orElseThrow() : "0";
  }

  private static String fingerprint(String... fields) {
    var framed = new StringBuilder();
    for (String field : fields) {
      if (field == null) framed.append("-1:");
      else framed.append(field.length()).append(':').append(field);
    }
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(framed.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException("Required SHA-256 unavailable");
    }
  }
}
