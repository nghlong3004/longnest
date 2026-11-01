package me.nghlong3004.longnest.service;

import me.nghlong3004.longnest.exception.ResourceException;

/** Provides replies to incoming chat messages. */
public interface ChatService {

  /**
   * Generates a raw JSON interpretation for the supplied user message.
   *
   * <p>Each call processes the message independently, without retaining conversation history. If no
   * chat client is configured, the original text is returned unchanged.
   *
   * <p>During JSON preview, the model's output is returned unchanged for inspection; it is not parsed,
   * corrected, or persisted.
   *
   * @param text the literal content of the user's message
   * @return the model's raw JSON response, or {@code text} when no chat client is configured
   * @throws ResourceException if the chatbot is unavailable or returns a null or blank response
   * @throws RuntimeException if an unexpected client resolution or processing error occurs
   */
  String reply(String text);
}
