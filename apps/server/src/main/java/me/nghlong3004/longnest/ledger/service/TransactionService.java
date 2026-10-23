package me.nghlong3004.longnest.ledger.service;

import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.ControlBinding;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionPatch;
import me.nghlong3004.longnest.ledger.model.ControlProof;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;

public interface TransactionService {
  /**
   * Initializes the sole owner head only in empty, durable storage; operator use only.
   *
   * @param context trusted configured principal with no transport operation or source time
   * @throws LedgerException for forbidden identity, invalid context or nonempty/unavailable store
   */
  void initializeOwner(HostContext context);

  /**
   * Lists at most ten frozen, uncompleted creates for an authenticated adapter namespace.
   *
   * @param context trusted principal; no source time or new operation is required
   * @param operationPrefix numeric Telegram bot namespace (tg:botId:), not a SQL pattern
   * @return immutable pending operation identities; no financial mutation
   * @throws LedgerException for forbidden identity, invalid prefix or unavailable storage/head
   */
  List<String> pendingCreates(HostContext context, String operationPrefix);

  /**
   * Authorizes and durably freezes a validated intent/reference, without creating a ledger entry.
   *
   * @param context authenticated host principal, stable operation ID and captured source reference
   * @param input create data only; defaults use the existing frozen reference on redelivery
   * @throws LedgerException for forbidden/invalid input, missing head or idempotency conflict
   */
  void ingest(HostContext context, TransactionInput input);

  /**
   * Authorizes and atomically commits or recovers one previously ingested create operation.
   *
   * @param context trusted principal and operation ID; frozen inbox supplies other values
   * @return receipt after proxy commit, or the labeled immutable original outcome on replay
   * @throws LedgerException for forbidden/missing data, invalid context or generation overflow
   */
  MutationReceipt create(HostContext context);

  /**
   * Recovers a frozen create before a host repeats nondeterministic interpretation.
   *
   * @param context trusted principal and original event operation identity
   * @return empty only when no create intent exists; otherwise its committed original receipt
   * @throws LedgerException for invalid/foreign context, conflicting controls or storage failure
   */
  Optional<MutationReceipt> recoverCreateIfPresent(HostContext context);

  /**
   * Authorizes and persists a field prompt for an owned active revision, without mutation.
   *
   * @param context trusted owner, operation identity and interaction scope
   * @param id target entry ID
   * @param revision expected business revision
   * @param field selected editable field
   * @param previousActionId optional pending preview whose after-state carries forward
   * @return frozen prompt; same operation and payload replay the original prompt
   * @throws LedgerException for unauthorized, stale, expired, conflicting or invalid input
   */
  FieldPrompt openFieldPrompt(HostContext context, String id, String revision, EditField field,
      String previousActionId);

  /**
   * Binds a pending prompt to the successfully displayed message; unbound prompts accept no reply.
   *
   * @param context trusted owner, original prompt operation and interaction scope
   * @param promptId frozen prompt identity
   * @param displayMessage positive host display-message identity
   * @throws LedgerException for unauthorized, expired, completed or mismatched prompt
   */
  void bindPrompt(HostContext context, String promptId, long displayMessage);

  /**
   * Validates a reply against its persisted prompt binding and freezes the replacement preview.
   *
   * @param context trusted owner, reply operation and interaction scope
   * @param replyToMessage displayed prompt message identity
   * @param value replacement field value; empty note clears it
   * @return immutable before/after preview; same reply operation/value replays it
   * @throws LedgerException for unauthorized, invalid, stale, expired or changed reply
   */
  ConfirmationRequired previewFieldReply(HostContext context, long replyToMessage, String value);

  /**
   * Authorizes and validates a complete after-state, then persists an immutable update preview.
   *
   * @param context trusted owner, operation identity and interaction scope
   * @param id owned target ID
   * @param revision expected business revision
   * @param patch sparse validated fields; no identity or creation-time replacement
   * @param previousActionId optional pending preview whose after-state carries forward
   * @return newest preview, or the original preview on identical operation replay
   * @throws LedgerException for unauthorized, invalid, stale or conflicting request
   */
  ConfirmationRequired previewUpdate(HostContext context, String id, String revision,
      TransactionPatch patch, String previousActionId);

  /**
   * Authorizes and freezes a target-displayed Delete review; never deletes on request.
   *
   * @param context trusted owner, operation identity and interaction scope
   * @param id owned active target ID
   * @param revision expected business revision
   * @return immutable target preview, including on identical operation replay
   * @throws LedgerException for unauthorized, stale, invalid or conflicting request
   */
  ConfirmationRequired requestDelete(HostContext context, String id, String revision);

  /**
   * Resolves the current owned revision once for /delete; replay never re-reads mutable state.
   *
   * @param context trusted owner, command operation identity and interaction scope
   * @param id owned active target ID
   * @return original frozen Delete preview on repeat, with no financial mutation
   * @throws LedgerException for unauthorized, missing, invalid or changed command identity
   */
  ConfirmationRequired requestDeleteCurrent(HostContext context, String id);

  /**
   * Freezes a deleted view's Restore button; preparation itself makes no financial change.
   *
   * @param context trusted owner and stable display operation/interaction scope
   * @param id owned deleted target ID
   * @param revision displayed expected business revision
   * @return durable button action, replayed for the same display operation
   * @throws LedgerException for unauthorized, stale, active, invalid or conflicting target
   */
  ConfirmationRequired openRestoreButton(HostContext context, String id, String revision);

  /**
   * Binds a pending action to its actual display message; no unbound action can commit.
   *
   * @param context trusted owner, original action operation and interaction scope
   * @param actionId frozen action identity
   * @param displayMessage positive host display-message identity
   * @throws LedgerException for unauthorized, expired, consumed or mismatched action
   */
  void bindAction(HostContext context, String actionId, long displayMessage);

  /**
   * Reads an owned control's display binding for authenticated host proof construction.
   *
   * @param context trusted owner and interaction scope
   * @param controlId opaque persisted control identity
   * @return stored kind, target, revision and bound display message
   * @throws LedgerException for unauthorized, foreign or missing control
   */
  ControlBinding controlBinding(HostContext context, String controlId);

  /**
   * Consumes a bound Save action atomically with its immutable update and receipt.
   *
   * @param context trusted owner, callback operation and interaction scope
   * @param actionId displayed update action identity
   * @param proof authenticated host proof for that action and message
   * @return committed receipt or original receipt on repeat clicks, including later callbacks
   * @throws LedgerException for unauthorized, wrong proof, stale, expired or conflicting action
   */
  MutationReceipt update(HostContext context, String actionId, ControlProof proof);

  /**
   * Consumes a target-bound Confirm Delete action atomically; a request alone cannot delete.
   *
   * @param context trusted owner, callback operation and interaction scope
   * @param actionId displayed Delete review identity
   * @param proof authenticated host proof for that target and message
   * @return committed or original replayed Delete receipt
   * @throws LedgerException for unauthorized, wrong proof, stale, expired or conflicting action
   */
  MutationReceipt confirmDelete(HostContext context, String actionId, ControlProof proof);

  /**
   * Restores on one authenticated displayed-button click; no second Save gate applies.
   *
   * @param context trusted owner, callback operation and interaction scope
   * @param actionId durable Restore button identity
   * @param proof authenticated host proof for that action and message
   * @return committed Restore receipt or original receipt on any later click
   * @throws LedgerException for unauthorized, wrong proof, stale, expired or conflicting action
   */
  MutationReceipt restoreAction(HostContext context, String actionId, ControlProof proof);

  /**
   * Restores by explicit authorized typed intent, retaining the caller's stable operation ID.
   *
   * @param context trusted owner, operation identity and interaction scope
   * @param id owned target ID
   * @param expectedRevision expected business revision
   * @return committed, active-state no-op or original replayed Restore receipt
   * @throws LedgerException for unauthorized, stale, invalid or conflicting request
   */
  MutationReceipt restore(HostContext context, String id, String expectedRevision);

  /**
   * Cancels a bound pending control without financial change; repeat Cancel is unchanged.
   *
   * @param context trusted owner, callback operation and interaction scope
   * @param controlId displayed prompt/action identity
   * @param proof authenticated host proof for that control and message
   * @throws LedgerException for unauthorized, wrong proof, expired or completed control
   */
  void cancel(HostContext context, String controlId, ControlProof proof);
}
