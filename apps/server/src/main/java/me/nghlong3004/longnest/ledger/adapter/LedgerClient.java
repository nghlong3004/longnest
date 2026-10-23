package me.nghlong3004.longnest.ledger.adapter;

import java.util.Optional;
import java.util.List;
import java.util.function.Supplier;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.dto.RangeSummary;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.ControlBinding;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.TransactionInput;
import me.nghlong3004.longnest.ledger.dto.TransactionPatch;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.ControlProof;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.service.LedgerQueryService;
import me.nghlong3004.longnest.ledger.service.TransactionService;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Local adapter: catches persistence/commit failures after transactional proxies exit. */
@Component
@Profile("ledger")
@RequiredArgsConstructor
public class LedgerClient {
  private final PersonalOwner owner;
  private final TransactionService transactions;
  private final LedgerQueryService queries;

  public LedgerResult<MutationReceipt> create(HostContext context, TransactionInput input) {
    return call(context, true, () -> {
      transactions.ingest(context, input);
      return transactions.create(context);
    });
  }

  public LedgerResult<MutationReceipt> recover(HostContext context) {
    return call(context, true, () -> transactions.create(context));
  }

  public LedgerResult<List<String>> pendingCreates(HostContext context, String operationPrefix) {
    return call(context, false, () -> transactions.pendingCreates(context, operationPrefix));
  }

  public LedgerResult<TransactionView> get(HostContext context, String id) {
    return call(context, false, () -> queries.get(context, id));
  }

  public LedgerResult<TransactionPage> recent(HostContext context, Optional<String> cursor) {
    return call(context, false, () -> queries.recent(context, cursor));
  }

  public LedgerResult<RangeSummary> aggregate(HostContext context, DateRange range,
      RangeFilter filter) {
    return call(context, false, () -> queries.aggregate(context, range, filter));
  }

  public LedgerResult<TransactionPage> list(HostContext context, DateRange range,
      RangeFilter filter, Optional<String> cursor) {
    return call(context, false, () -> queries.list(context, range, filter, cursor));
  }

  public LedgerResult<FieldPrompt> openFieldPrompt(HostContext context, String id,
      String revision, EditField field, String previousActionId) {
    return call(context, true, () -> transactions.openFieldPrompt(context, id, revision,
        field, previousActionId));
  }

  public LedgerResult<Boolean> bindPrompt(HostContext context, String id, long message) {
    return call(context, true, () -> {
      transactions.bindPrompt(context, id, message);
      return true;
    });
  }

  public LedgerResult<ConfirmationRequired> previewFieldReply(HostContext context,
      long replyToMessage, String value) {
    return call(context, true,
        () -> transactions.previewFieldReply(context, replyToMessage, value));
  }

  public LedgerResult<ConfirmationRequired> previewUpdate(HostContext context, String id,
      String revision, TransactionPatch patch, String previousActionId) {
    return call(context, true,
        () -> transactions.previewUpdate(context, id, revision, patch, previousActionId));
  }

  public LedgerResult<ConfirmationRequired> requestDelete(HostContext context,
      String id, String revision) {
    return call(context, true, () -> transactions.requestDelete(context, id, revision));
  }

  public LedgerResult<ConfirmationRequired> requestDeleteCurrent(HostContext context, String id) {
    return call(context, true, () -> transactions.requestDeleteCurrent(context, id));
  }

  public LedgerResult<ConfirmationRequired> openRestoreButton(HostContext context, String id,
      String revision) {
    return call(context, true, () -> transactions.openRestoreButton(context, id, revision));
  }

  public LedgerResult<Boolean> bindAction(HostContext context, String id, long message) {
    return call(context, true, () -> {
      transactions.bindAction(context, id, message);
      return true;
    });
  }

  public LedgerResult<ControlBinding> controlBinding(HostContext context, String id) {
    return call(context, false, () -> transactions.controlBinding(context, id));
  }

  public LedgerResult<MutationReceipt> update(HostContext context, String id,
      ControlProof proof) {
    return call(context, true, () -> transactions.update(context, id, proof));
  }

  public LedgerResult<MutationReceipt> confirmDelete(HostContext context, String id,
      ControlProof proof) {
    return call(context, true, () -> transactions.confirmDelete(context, id, proof));
  }

  public LedgerResult<MutationReceipt> restoreAction(HostContext context, String id,
      ControlProof proof) {
    return call(context, true, () -> transactions.restoreAction(context, id, proof));
  }

  public LedgerResult<MutationReceipt> restore(HostContext context, String id,
      String expectedRevision) {
    return call(context, true, () -> transactions.restore(context, id, expectedRevision));
  }

  public LedgerResult<Boolean> cancel(HostContext context, String id, ControlProof proof) {
    return call(context, true, () -> {
      transactions.cancel(context, id, proof);
      return true;
    });
  }

  private <T> LedgerResult<T> call(HostContext context, boolean mutation, Supplier<T> action) {
    try {
      owner.authorize(context);
      // Prevent nesting independent units beneath a caller that may hold owner/entry locks.
      if (TransactionSynchronizationManager.isActualTransactionActive()) {
        throw new LedgerException(LedgerException.Code.INVALID_STATE);
      }
      return new LedgerResult.Ok<>(action.get());
    } catch (RuntimeException exception) {
      Optional<String> operation = mutation && context != null
          ? Optional.ofNullable(context.operationId()) : Optional.empty();
      return failure(exception, operation, mutation);
    }
  }

  private static <T> LedgerResult.Failure<T> failure(
      RuntimeException exception, Optional<String> operationId, boolean mutation) {
    LedgerException.Code code;
    if (exception instanceof LedgerException business) {
      code = business.getCode();
    } else if (exception instanceof DataIntegrityViolationException
        || exception instanceof ConcurrencyFailureException
        || exception instanceof UnexpectedRollbackException) {
      code = LedgerException.Code.NOT_SAVED;
    } else {
      // Unclassified mutation infrastructure failures are conservative: never infer a rollback.
      code = mutation ? LedgerException.Code.OUTCOME_UNKNOWN : LedgerException.Code.NOT_SAVED;
    }
    boolean retryable = code == LedgerException.Code.NOT_SAVED
        || code == LedgerException.Code.OUTCOME_UNKNOWN;
    return new LedgerResult.Failure<>(code, operationId, retryable);
  }
}
