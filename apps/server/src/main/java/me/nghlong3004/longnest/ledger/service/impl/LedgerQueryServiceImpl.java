package me.nghlong3004.longnest.ledger.service.impl;

import static me.nghlong3004.longnest.ledger.model.LedgerException.Code.NOT_FOUND_OR_FORBIDDEN;

import java.util.Optional;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.dto.RangeSummary;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import me.nghlong3004.longnest.ledger.repository.LedgerStore;
import me.nghlong3004.longnest.ledger.service.LedgerQueryService;
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
public class LedgerQueryServiceImpl implements LedgerQueryService {
  private final PersonalOwner owner;
  private final LedgerStore store;
  private final TransactionMapper mapper;
  private final LedgerCursors cursors;

  @Override
  public TransactionView get(HostContext context, String id) {
    String principal = owner.authorize(context);
    long key;
    try {
      if (id == null || !id.matches("[1-9][0-9]{0,18}")) {
        throw new NumberFormatException();
      }
      key = Long.parseLong(id);
    } catch (NumberFormatException exception) {
      throw new LedgerException(NOT_FOUND_OR_FORBIDDEN);
    }
    store.lockHead(principal);
    return mapper.view(store.entry(principal, key).filter(entry -> principal.equals(entry.owner()))
        .orElseThrow(() -> new LedgerException(NOT_FOUND_OR_FORBIDDEN)));
  }

  @Override
  public TransactionPage recent(HostContext context, Optional<String> cursor) {
    String principal = owner.authorize(context);
    long generation = store.lockHead(principal);
    var before = cursors.resolve(principal, generation, cursor);
    var found = store.recent(principal, before);
    if (found.stream().anyMatch(entry -> !principal.equals(entry.owner()) || entry.deleted())) {
      throw new LedgerException(LedgerException.Code.NOT_SAVED);
    }
    var visible = found.subList(0, Math.min(10, found.size()));
    Optional<String> next = Optional.empty();
    if (found.size() > 10) {
      var last = visible.getLast();
      next = Optional.of(cursors.issue(principal, generation,
          new LedgerCursors.Key(last.createdAt(), last.id())));
    }
    return mapper.page(visible, generation, next);
  }

  @Override
  public RangeSummary aggregate(HostContext context, DateRange range, RangeFilter filter) {
    String principal = owner.authorize(context);
    validateRange(range, filter);
    long generation = store.lockHead(principal);
    return mapper.summary(range, filter, store.totals(principal, range, filter), generation);
  }

  @Override
  public TransactionPage list(HostContext context, DateRange range, RangeFilter filter,
      Optional<String> cursor) {
    String principal = owner.authorize(context);
    validateRange(range, filter);
    long generation = store.lockHead(principal);
    var before = cursors.resolve(principal, generation, Optional.of(range), filter, cursor);
    var totals = store.totals(principal, range, filter);
    var found = store.range(principal, range, filter, before);
    if (found.size() > 11 || found.stream().anyMatch(entry ->
        !principal.equals(entry.owner()) || entry.deleted()
            || !filter.includes(entry.intent().draft().type(), entry.intent().draft().category())
            || entry.intent().transactionDate().isBefore(range.from())
            || entry.intent().transactionDate().isAfter(range.to()))) {
      throw new LedgerException(LedgerException.Code.NOT_SAVED);
    }
    var visible = found.subList(0, Math.min(10, found.size()));
    Optional<String> next = Optional.empty();
    if (found.size() > 10) {
      var last = visible.getLast();
      next = Optional.of(cursors.issue(principal, generation, Optional.of(range), filter,
          new LedgerCursors.Key(null, last.id(), last.intent().transactionDate())));
    }
    return mapper.rangePage(visible, mapper.summary(range, filter, totals, generation), next);
  }

  private static void validateRange(DateRange range, RangeFilter filter) {
    if (range == null) throw new LedgerException(LedgerException.Code.INVALID_RANGE);
    if (filter == null) throw new LedgerException(LedgerException.Code.INVALID_FIELD);
  }
}
