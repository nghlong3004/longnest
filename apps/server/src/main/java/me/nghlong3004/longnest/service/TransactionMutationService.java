package me.nghlong3004.longnest.service;

import java.util.UUID;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionRevision;
import me.nghlong3004.longnest.model.enums.TransactionField;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

/** Validates and commits owner-scoped transaction changes after explicit user confirmation. */
public interface TransactionMutationService {

  /**
   * Persists a source-only receipt so edit input cannot later create a transaction on replay.
   *
   * <p>Stores identifiers only. Financial rows, creation drafts and their payloads are unchanged.
   *
   * @param source trusted sender, channel, chat and incoming message metadata
   * @return true for a newly consumed source; false if the source was already consumed
   * @throws IllegalArgumentException if source metadata is invalid or the identity does not exist
   * @throws DataAccessException if receipt lookup or persistence fails
   * @throws TransactionException if the receipt transaction cannot start or commit
   */
  boolean consumeInput(ChatMessage source);

  /**
   * Loads an owned transaction and its version for a subsequent preview or confirmation.
   *
   * <p>Checks owner, channel and chat without creating an identity, draft or transaction. The row
   * lock ends with this call's transaction; later writes must recheck the returned version.
   *
   * @param source trusted sender and conversation metadata from the authorized adapter
   * @param id stable transaction ID selected by the user
   * @return the current transaction snapshot and its optimistic-lock version
   * @throws IllegalArgumentException if metadata is invalid or the transaction is unavailable
   * @throws DataAccessException if the lookup or row lock fails
   */
  TransactionRevision load(ChatMessage source, UUID id);

  /**
   * Parses one field replacement and validates the proposed transaction without writing data.
   *
   * <p>Preserves untouched values. Switching to income clears the expense category; this derived
   * change must also appear in the user's preview. The caller must not treat a preview as a save.
   *
   * @param before non-null versioned snapshot previously loaded for the selected transaction
   * @param field editable field selected by the user
   * @param input literal replacement, up to 500 characters; {@code -} clears an optional field
   * @return validated proposed data, including any category change derived from the new type
   * @throws IllegalArgumentException if the field, input or proposed financial values are invalid
   */
  TransactionData preview(TransactionRevision before, TransactionField field, String input);

  /**
   * Rechecks ownership and version, then atomically saves the user's confirmed preview.
   *
   * <p>A successful return through the transactional service proxy precedes Telegram updates.
   * Creation drafts and source provenance are preserved.
   *
   * @param source trusted sender and conversation metadata from the authorized adapter
   * @param id stable ID of the transaction being edited
   * @param version version used to build the confirmed preview
   * @param data validated financial values shown in the complete preview
   * @throws IllegalArgumentException if ownership, version or proposed values no longer match
   * @throws DataAccessException if locking or persistence fails
   * @throws TransactionException if the transaction cannot start or commit
   */
  void save(ChatMessage source, UUID id, long version, TransactionData data);

  /**
   * Permanently deletes the confirmed transaction after rechecking ownership and version.
   *
   * <p>Atomically detaches and clears any confirmed draft and retains source-only replay receipts,
   * preventing old messages from recreating the erased entry. Telegram card removal happens later
   * and cannot roll back the committed ledger deletion.
   *
   * @param source trusted sender and conversation metadata from the authorized adapter
   * @param id stable ID of the transaction whose permanent deletion was confirmed
   * @param version version shown in the deletion confirmation
   * @throws IllegalArgumentException if ownership, version or transaction availability changed
   * @throws DataAccessException if locking, receipt persistence or deletion fails
   * @throws TransactionException if the transaction cannot start or commit
   */
  void delete(ChatMessage source, UUID id, long version);
}
