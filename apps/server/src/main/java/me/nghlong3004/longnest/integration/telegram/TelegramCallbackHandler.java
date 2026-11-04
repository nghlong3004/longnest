package me.nghlong3004.longnest.integration.telegram;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/** Handles a callback after the message adapter has authenticated its sender and chat. */
public interface TelegramCallbackHandler {
  boolean supports(String data);

  void handleCallback(CallbackQuery callback, Message message);
}
