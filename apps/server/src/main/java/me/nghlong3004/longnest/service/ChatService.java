package me.nghlong3004.longnest.service;

import java.util.UUID;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.DraftCancellation;

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
