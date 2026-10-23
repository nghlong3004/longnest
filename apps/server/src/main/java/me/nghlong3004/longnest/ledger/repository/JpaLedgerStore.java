package me.nghlong3004.longnest.ledger.repository;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.entity.CreateInbox;
import me.nghlong3004.longnest.ledger.model.entity.LedgerControl;
import me.nghlong3004.longnest.ledger.model.entity.ControlEvent;
import me.nghlong3004.longnest.ledger.model.entity.InboxId;
import me.nghlong3004.longnest.ledger.model.entity.LedgerEntry;
import me.nghlong3004.longnest.ledger.model.entity.OwnerHead;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("ledger")
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class JpaLedgerStore implements LedgerStore {
  private final OwnerHeadRepository heads;
  private final EntryRepository entries;
  private final InboxRepository inboxes;
  private final ControlRepository controls;
  private final ControlEventRepository controlEvents;
  private final EntityManager entityManager;

  @Override
  public void initializeOwner(String owner) {
    initializeOwner(owner, false);
  }

  @Override
  public void prepareOwner(String owner) {
    initializeOwner(owner, true);
  }

  private void initializeOwner(String owner, boolean allowExisting) {
    // One-time initialization must serialize even two different proposed owners.
    entityManager.createNativeQuery(
        "lock table ledger_owner_head, ledger_entry, ledger_create_inbox, ledger_control, ledger_control_event in exclusive mode")
        .executeUpdate();
    requireDurability();
    if (allowExisting && heads.count() == 1 && heads.existsById(owner)) return;
    if (heads.count() != 0 || entries.count() != 0 || inboxes.count() != 0
        || controls.count() != 0 || controlEvents.count() != 0) {
      throw unavailable();
    }
    heads.saveAndFlush(new OwnerHead(owner));
  }

  @Override
  public long lockHead(String owner) {
    var head = heads.lockOwner(owner).orElseThrow(JpaLedgerStore::unavailable);
    requireDurability();
    return head.getGeneration();
  }

  private void requireDurability() {
    Object durability = entityManager.createNativeQuery(
        "select current_setting('fsync') || ':' || current_setting('synchronous_commit')")
        .getSingleResult();
    if (!"on:on".equals(durability)) {
      throw unavailable();
    }
  }

  @Override
  public Optional<Inbox> inbox(String owner, String operationId) {
    return inboxes.findById(new InboxId(owner, operationId)).map(CreateInbox::snapshot);
  }

  @Override
  public List<String> pendingCreates(String owner, String operationPrefix) {
    return List.copyOf(inboxes.pending(owner, operationPrefix, PageRequest.of(0, 10)));
  }

  @Override
  public void freeze(Inbox inbox) {
    inboxes.save(new CreateInbox(inbox));
  }

  @Override
  public Entry insert(
      String owner, CreateIntent intent, Instant createdAt, Optional<Instant> messageAt) {
    return entries.saveAndFlush(new LedgerEntry(owner, intent, createdAt, messageAt)).snapshot();
  }

  @Override
  public void advance(String owner, long expectedGeneration, long nextGeneration) {
    heads.findById(owner).orElseThrow(JpaLedgerStore::unavailable)
        .advance(expectedGeneration, nextGeneration);
  }

  @Override
  public void complete(String owner, String operationId, Receipt receipt) {
    inboxes.findById(new InboxId(owner, operationId)).orElseThrow(JpaLedgerStore::unavailable)
        .complete(receipt);
  }

  @Override
  public Optional<Entry> entry(String owner, long id) {
    return entries.findByOwnerAndId(owner, id).map(LedgerEntry::snapshot);
  }

  @Override
  public List<Entry> recent(String owner, Optional<LedgerCursors.Key> before) {
    List<LedgerEntry> found = before.isEmpty()
        ? entries.findTop11ByOwnerAndDeletedFalseOrderByCreatedAtDescIdDesc(owner)
        : entries.after(owner, before.orElseThrow().createdAt(), before.orElseThrow().id(),
            PageRequest.of(0, 11));
    return found.stream().map(LedgerEntry::snapshot).toList();
  }

  @Override
  public Totals totals(String owner, DateRange range, RangeFilter filter) {
    var values = entries.totals(owner, range.from(), range.to(), filter.name());
    return new Totals(values.getIncomeTotal().toBigIntegerExact(),
        values.getIncomeCount().toBigIntegerExact(), values.getExpenseTotal().toBigIntegerExact(),
        values.getExpenseCount().toBigIntegerExact());
  }

  @Override
  public List<Entry> range(String owner, DateRange range, RangeFilter filter,
      Optional<LedgerCursors.Key> before) {
    return entries.range(owner, range.from(), range.to(), filter.name(),
        before.map(LedgerCursors.Key::date).orElse(null),
        before.map(LedgerCursors.Key::id).orElse(0L), PageRequest.of(0, 11))
        .stream().map(LedgerEntry::snapshot).toList();
  }

  @Override
  public Optional<Control> control(String owner, String id) {
    return controls.findByOwnerAndId(owner, id).map(LedgerControl::snapshot);
  }

  @Override
  public Optional<Control> controlByRequest(String owner, String requestId) {
    return controls.findByOwnerAndRequestId(owner, requestId)
        .map(LedgerControl::snapshot);
  }

  @Override
  public Optional<String> controlEventFingerprint(String owner, String operationId) {
    return controlEvents.findById(new InboxId(owner, operationId))
        .map(ControlEvent::fingerprint);
  }

  @Override
  public void bindControlEvent(String owner, String operationId, String fingerprint,
      String controlId) {
    controlEvents.saveAndFlush(ControlEvent.bound(owner, operationId, fingerprint, controlId));
  }

  @Override
  public Optional<Control> promptForReply(String owner, String scope, long displayMessage) {
    return controls.findByOwnerAndScopeAndDisplayMessageAndKind(
        owner, scope, displayMessage, ControlKind.PROMPT).stream()
        .findFirst().map(LedgerControl::snapshot);
  }

  @Override
  public long liveControls(String owner, Instant now) {
    return controls.countByOwnerAndStatusAndExpiresAtAfter(owner, ControlStatus.PENDING,
        now);
  }

  @Override
  public void addControl(Control control) {
    controls.saveAndFlush(LedgerControl.frozen(control));
  }

  @Override
  public void bindControl(String owner, String id, long displayMessage) {
    controls.findByOwnerAndId(owner, id).orElseThrow(JpaLedgerStore::unavailable)
        .bind(displayMessage);
  }

  @Override
  public void finishControl(String owner, String id, ControlStatus status, String successorId,
      Long generation, Boolean changed) {
    controls.findByOwnerAndId(owner, id).orElseThrow(JpaLedgerStore::unavailable)
        .finish(status, successorId, generation, changed);
  }

  @Override
  public void replacePendingActions(String owner, String scope, long targetId) {
    for (LedgerControl action : controls.findByOwnerAndScopeAndTargetIdAndStatusAndKindNot(
        owner, scope, targetId, ControlStatus.PENDING, ControlKind.PROMPT)) {
      action.finish(ControlStatus.REPLACED, null, null, null);
    }
  }

  @Override
  public Entry mutate(String owner, long id, long expectedRevision, CreateIntent after,
      boolean deleted) {
    var entity = entries.findByOwnerAndId(owner, id).orElseThrow(JpaLedgerStore::unavailable);
    entity.mutate(expectedRevision, after, deleted);
    return entity.snapshot();
  }

  private static LedgerException unavailable() {
    return new LedgerException(LedgerException.Code.NOT_SAVED);
  }
}
