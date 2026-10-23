package me.nghlong3004.longnest.ledger.repository;

import java.time.Instant;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.ledger.model.CreateIntent;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;

/** Persistence port inside service transactions. Implementations never return entities. */
public interface LedgerStore {
  void initializeOwner(String owner);

  long lockHead(String owner);

  Optional<Inbox> inbox(String owner, String operationId);

  List<String> pendingCreates(String owner, String operationPrefix);

  void freeze(Inbox inbox);

  Entry insert(String owner, CreateIntent intent, Instant createdAt, Optional<Instant> messageAt);

  void advance(String owner, long expectedGeneration, long nextGeneration);

  void complete(String owner, String operationId, Receipt receipt);

  Optional<Entry> entry(String owner, long id);

  List<Entry> recent(String owner, Optional<LedgerCursors.Key> before);

  Totals totals(String owner, DateRange range, RangeFilter filter);

  List<Entry> range(String owner, DateRange range, RangeFilter filter,
      Optional<LedgerCursors.Key> before);

  Optional<Control> control(String owner, String id);

  Optional<Control> controlByRequest(String owner, String requestId);

  Optional<String> controlEventFingerprint(String owner, String operationId);

  void bindControlEvent(String owner, String operationId, String fingerprint,
      String controlId);

  Optional<Control> promptForReply(String owner, String scope, long displayMessage);

  long liveControls(String owner, Instant now);

  void addControl(Control control);

  void bindControl(String owner, String id, long displayMessage);

  void finishControl(String owner, String id, ControlStatus status, String successorId,
      Long generation, Boolean changed);

  void replacePendingActions(String owner, String scope, long targetId);

  Entry mutate(String owner, long id, long expectedRevision, CreateIntent after, boolean deleted);

  record Receipt(long entryId, long generation, Instant createdAt) {}

  record Totals(BigInteger incomeTotal, BigInteger incomeCount,
      BigInteger expenseTotal, BigInteger expenseCount) {
    public Totals {
      for (BigInteger value : List.of(incomeTotal, incomeCount, expenseTotal, expenseCount)) {
        if (value.signum() < 0) throw new IllegalArgumentException("Negative aggregate");
      }
    }

    public BigInteger net() { return incomeTotal.subtract(expenseTotal); }
  }

  record Inbox(String owner, String operationId, String fingerprint, CreateIntent intent,
      Instant businessReference, Optional<Instant> messageAt, Optional<Receipt> receipt) {}

  record Entry(String owner, long id, long revision, CreateIntent intent,
      Instant createdAt, Optional<Instant> messageAt, boolean deleted) {}

  enum ControlKind { PROMPT, UPDATE, DELETE, RESTORE }

  enum ControlStatus { PENDING, FILLED, REPLACED, CANCELED, COMPLETED }

  record Control(String id, String owner, String scope, String requestId, ControlKind kind,
      ControlStatus status, EditField field, String originId, String successorId,
      Entry before, Entry after, Instant expiresAt, Long displayMessage,
      Long generation, Boolean changed, String requestFingerprint) {}
}
