package me.nghlong3004.longnest.service;

import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;

/** Provides replies to incoming chat messages. */
public interface ChatService {

  /**
   * Interprets a message and records a transaction when the model returns valid READY data.
   *
   * <p>Each call processes the message independently, without retaining conversation history. If no
   * chat client is configured, the original text is returned unchanged without database writes.
   *
   * <p>Clarification and ordinary chat return the model's reply without ledger writes. A READY reply
   * is generated from the stored transaction only after the database commit succeeds.
   *
   * @param message literal text and trusted sender, source, and original timestamp
   * @return a user-facing reply, a committed-transaction confirmation, or the original text in echo mode
   * @throws ResourceException if the provider fails, returns empty/invalid data, or persistence fails
   * @throws IllegalArgumentException if the incoming message metadata is invalid
   * @throws RuntimeException if an unexpected client resolution or processing error occurs
   */
  String reply(ChatMessage message);
}
