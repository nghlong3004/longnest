package me.nghlong3004.longnest.service;

import java.util.UUID;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.DraftCancellation;
import me.nghlong3004.longnest.model.dto.TransactionPageRequest;

/** Provides replies to incoming chat messages. */
public interface ChatService {

  /**
   * Interprets a message and records a transaction when the model returns valid READY data.
   *
   * <p>Replies use the pending draft or explicitly replied-to question as context. If no chat
   * client is configured, text is echoed. Cancellation commands work without calling the model.
   *
   * <p>Clarification persists a draft and returns its ID for a cancel button. READY confirms only
   * after commit; ordinary chat preserves any current draft without changing financial data.
   *
   * @param message literal text and trusted sender, source, and original timestamp
   * @return a user-facing reply, a committed-transaction confirmation, or the original text in echo
   *     mode
   * @throws ResourceException if the provider fails, returns empty/invalid data, or persistence
   *     fails
   * @throws IllegalArgumentException if the incoming message metadata is invalid
   * @throws RuntimeException if an unexpected client resolution or processing error occurs
   */
  ChatReply reply(ChatMessage message);

  /**
   * Retains a completed reply, including an adapter-localized error, as bounded history only.
   * Duplicate source delivery does not add an exchange or grant financial authority.
   */
  void rememberReply(ChatMessage message, ChatReply reply);

  /**
   * Retrieves another bounded transaction page without calling the model or changing a draft.
   *
   * @param message trusted sender and conversation; owner is never supplied by the callback data
   * @param request fixed inclusive dates, transaction type, cursor and original query message ID
   * @return database totals and stored transactions for this sender only
   * @throws ResourceException if the filter/cursor is invalid or database access fails
   * @throws IllegalArgumentException if the incoming sender metadata is invalid
   */
  ChatReply nextPage(ChatMessage message, TransactionPageRequest request);

  /**
   * Registers the actual ID returned when the adapter delivers a clarification question.
   *
   * @param message authorized incoming source
   * @param reply pending draft and question text produced by {@link #reply(ChatMessage)}
   * @param questionMessageId actual sent question ID
   * @throws ResourceException if the draft is absent or not owned by this sender/chat
   */
  void registerQuestion(ChatMessage message, ChatReply reply, String questionMessageId);

  /**
   * Cancels an explicit button target, or the current draft for a text command, without a model
   * call.
   *
   * @param message authorized sender and conversation
   * @param draftId button target, or null to resolve the current draft
   * @return the committed status, descriptive notification, original source and whether this call
   *     changed the draft; a confirmed transaction is unchanged
   * @throws ResourceException if the target is not owned by this sender/chat or persistence fails
   */
  DraftCancellation cancelDraft(ChatMessage message, UUID draftId);
}
