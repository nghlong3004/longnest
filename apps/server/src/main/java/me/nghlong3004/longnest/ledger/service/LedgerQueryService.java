package me.nghlong3004.longnest.ledger.service;

import java.util.Optional;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.ledger.dto.RangeSummary;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.LedgerException;

public interface LedgerQueryService {
  /**
   * Authorizes before selecting an owned active or deleted record; never changes the ledger.
   *
   * @param context authenticated host identity, not supplied by transaction text
   * @param id positive canonical decimal ID within signed BIGINT bounds
   * @return owned immutable current-state view; absence and foreign IDs are indistinguishable
   * @throws LedgerException for unavailable/forbidden ID or missing established head
   */
  TransactionView get(HostContext context, String id);

  /**
   * Authorizes and reads at most ten active records under the owner-head consistency lock.
   *
   * @param context authenticated host identity
   * @param cursor absent for page one, otherwise an owner/generation-bound recent cursor
   * @return page ordered by saved creation instant then ID descending, with an opaque next token
   * @throws LedgerException for forbidden access, missing head or invalid/stale cursor (refresh)
   */
  TransactionPage recent(HostContext context, Optional<String> cursor);

  /**
   * Reads exact full-range active totals under the owner-head consistency lock.
   *
   * @param context authenticated host identity
   * @param range validated inclusive financial dates
   * @param filter all, income or expense; never inferred from transaction text
   * @return exact totals/counts/net and observed generation; no financial mutation
   * @throws LedgerException for unauthorized identity, missing head or invalid range/filter
   */
  RangeSummary aggregate(HostContext context, DateRange range, RangeFilter filter);

  /**
   * Reads a selectable range page and its full-range summary in one locked unit of work.
   *
   * @param context authenticated host identity
   * @param range validated inclusive financial dates
   * @param filter all, income or expense
   * @param cursor absent for page one, otherwise bound to owner/range/filter/generation
   * @return at most ten entries ordered date/ID descending with same-snapshot summary
   * @throws LedgerException for unauthorized identity, invalid input or invalid/stale cursor
   */
  TransactionPage list(HostContext context, DateRange range, RangeFilter filter,
      Optional<String> cursor);
}
