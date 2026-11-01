package me.nghlong3004.longnest.service;

import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.entity.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

/** Persists transactions proposed by the chatbot after application validation. */
public interface TransactionService {

  /**
   * Resolves the sender's internal user and records a transaction atomically.
   *
   * <p>A missing transaction date uses the original message's date in the ledger time zone.
   * Reprocessing a source already recorded for this user returns its existing transaction.
   * The write commits before the caller receives a successful return.
   *
   * @param data validated details containing a non-null type and positive amount
   * @param message trusted metadata from the authorized channel adapter
   * @return the committed transaction, including an existing one on replay
   * @throws DataAccessException if a database operation or commit fails
   * @throws TransactionException if the transaction cannot start or commit
   * @throws IllegalArgumentException if the supplied data violates entity invariants
   */
  Transaction record(TransactionData data, ChatMessage message);
}
