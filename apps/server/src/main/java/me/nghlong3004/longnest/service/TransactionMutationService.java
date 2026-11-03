package me.nghlong3004.longnest.service;

import java.util.UUID;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionRevision;

/** Deterministic owner/chat-scoped edits and permanent ledger deletion, never model-directed. */
public interface TransactionMutationService {
  /**
   * Records only source IDs; returns false for a replay. Never changes financial data or drafts.
   */
  boolean consumeInput(ChatMessage source);

  /** Loads a versioned snapshot without creating an identity or draft. */
  TransactionRevision load(ChatMessage source, UUID id);

  /** Validates one supplied field and preserves every untouched stored value. No writes. */
  TransactionData preview(TransactionRevision before, String field, String input);

  /** Commits a validated preview only while the owner/chat and expected version still match. */
  void save(ChatMessage source, UUID id, long version, TransactionData data);

  /** Erases the ledger row and detaches its draft atomically, retaining replay-only receipts. */
  void delete(ChatMessage source, UUID id, long version);
}
