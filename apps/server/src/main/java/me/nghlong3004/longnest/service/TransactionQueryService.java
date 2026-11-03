package me.nghlong3004.longnest.service;

import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionCursor;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import org.springframework.dao.DataAccessException;

/** Reads owner-scoped transaction pages and exact totals without modifying financial state. */
public interface TransactionQueryService {

  /**
   * Resolves dates using the message time zone and reads a bounded page in a consistent DB
   * snapshot. Relative periods use the incoming message date; both bounds are inclusive. Missing
   * identities yield an empty page and are not created. Existing drafts and receipts are unchanged.
   *
   * @param message trusted sender/channel metadata and original query timestamp
   * @param query model or decoded callback filter; owner IDs are never accepted from it
   * @param cursor last displayed key, or null for the first page
   * @return at most five entries, full-filter aggregates, and the next key if more exist
   * @throws ResourceException if the filter or cursor is invalid
   * @throws DataAccessException if reading the database fails
   */
  TransactionPage view(ChatMessage message, TransactionQuery query, TransactionCursor cursor);
}
