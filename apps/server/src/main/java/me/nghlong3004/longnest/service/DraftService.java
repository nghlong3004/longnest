package me.nghlong3004.longnest.service;

import java.util.Optional;
import java.util.UUID;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.ChatResponse;
import me.nghlong3004.longnest.model.dto.DraftContext;
import org.springframework.dao.DataAccessException;

/** Owns persistent clarification state, message routing and draft lifecycle changes. */
public interface DraftService {

  /**
   * Finds a processed source, an explicitly replied-to draft, or the pending draft in this chat.
   *
   * @param message authorized sender, chat and reply metadata
   * @return an owner-scoped snapshot; empty for a new conversation or unknown explicit reply
   * @throws DataAccessException if loading the draft fails
   */
  Optional<DraftContext> findContext(ChatMessage message);

  /**
   * Applies validated financial output to a new or existing draft in one transaction. Known fields
   * survive null output; a READY result records and confirms atomically. Stale or terminal contexts
   * return current state without applying obsolete output.
   *
   * @param response validated ASK_CLARIFICATION or READY output
   * @param message authorized incoming message
   * @param expected snapshot used by the model, or null for an initial question
   * @return the committed draft snapshot
   * @throws ResourceException if the draft is not owned by the sender or merged data is invalid
   * @throws DataAccessException if persistence fails; all changes roll back
   */
  DraftContext apply(ChatResponse response, ChatMessage message, DraftContext expected);

  /**
   * Links a delivered question to its draft, retaining earlier links for old Replies.
   *
   * @param message authorized source that produced the reply
   * @param reply the pending-draft reply sent by the adapter
   * @param questionMessageId actual message ID returned by the channel
   * @throws ResourceException if the draft is not owned by the sender
   * @throws DataAccessException if persistence fails
   */
  void registerQuestion(ChatMessage message, ChatReply reply, String questionMessageId);

  /**
   * Cancels a pending draft without deleting its data or changing a confirmed transaction.
   *
   * @param message authorized sender and chat metadata
   * @param draftId explicit button target, or null to resolve from the message/current draft
   * @return the committed state; empty if no current draft exists for a command
   * @throws ResourceException if an explicit target is not owned by this sender/chat
   * @throws DataAccessException if persistence fails
   */
  Optional<DraftContext> cancel(ChatMessage message, UUID draftId);
}
