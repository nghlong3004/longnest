package me.nghlong3004.longnest.service;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionRecordingResult;
import me.nghlong3004.longnest.model.entity.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

/** Persists transactions proposed by the chatbot after application validation. */
public interface TransactionService {

  /**
   * Finds a recorded message for this owner without creating identity or ledger records.
   *
   * @param message trusted sender and source metadata
   * @return the committed transaction for this source, if any
   * @throws DataAccessException if the lookup fails
   */
  Optional<TransactionData> findRecorded(ChatMessage message);

  /**
   * Atomically records a new context-free READY result or resumes a draft that won the race. The
   * resumed draft receives a receipt for this source before the transaction commits.
   *
   * @param data validated READY details
   * @param message trusted incoming source and sender
   * @return committed financial details or the committed draft snapshot for this source
   * @throws DataAccessException if lookup, receipt creation or recording fails
   */
  TransactionRecordingResult recordOrContinue(TransactionData data, ChatMessage message);

  /**
   * Resolves the sender's internal user and records a transaction atomically.
   *
   * <p>A missing transaction date uses the original message's date in the ledger time zone.
   * Reprocessing a source already recorded for this user returns its existing transaction. The
   * write commits before the caller receives a successful return.
   *
   * @param data validated details containing a non-null type and positive amount
   * @param message trusted metadata from the authorized channel adapter
   * @return the committed transaction, including an existing one on replay
   * @throws DataAccessException if a database operation or commit fails
   * @throws TransactionException if the transaction cannot start or commit
   * @throws IllegalArgumentException if the supplied data violates entity invariants
   */
  Transaction record(TransactionData data, ChatMessage message);

  /**
   * Records the owned pending draft's original source within the caller's confirmation transaction.
   *
   * @param data validated, merged draft details
   * @param originalMessage trusted original source and owner metadata
   * @param draftId pending draft being confirmed
   * @return the persisted transaction; the caller confirms the draft before committing
   * @throws ResourceException if draft/source state no longer matches
   * @throws DataAccessException if persistence fails
   */
  Transaction recordFromDraft(TransactionData data, ChatMessage originalMessage, UUID draftId);
}
