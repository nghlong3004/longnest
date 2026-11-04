package me.nghlong3004.longnest.integration.telegram;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.ReplyParameters;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Telegram delivery boundary; financial services never depend on transport outcomes. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramMessageSender {
  private static final int MAX_MESSAGE_LENGTH = 4096;
  private final TelegramClient client;

  public enum KeyboardPlacement {
    FIRST,
    LAST
  }

  public Message send(String chat, int original, String text, InlineKeyboardMarkup keyboard)
      throws TelegramApiException {
    var request = new SendMessage(chat, text);
    request.setReplyParameters(
        ReplyParameters.builder().messageId(original).allowSendingWithoutReply(true).build());
    request.setReplyMarkup(keyboard);
    return client.execute(request);
  }

  public List<Message> sendText(
      String chat,
      int original,
      String text,
      InlineKeyboardMarkup keyboard,
      KeyboardPlacement placement)
      throws TelegramApiException {
    var chunks = split(text);
    var sent = new ArrayList<Message>(chunks.size());
    int controls = placement == KeyboardPlacement.FIRST ? 0 : chunks.size() - 1;
    for (int index = 0; index < chunks.size(); index++) {
      sent.add(send(chat, original, chunks.get(index), index == controls ? keyboard : null));
    }
    return sent;
  }

  public boolean edit(String chat, int id, String text, InlineKeyboardMarkup keyboard) {
    if (id <= 0) {
      return false;
    }
    try {
      return !Boolean.FALSE.equals(
          client.execute(
              EditMessageText.builder()
                  .chatId(chat)
                  .messageId(id)
                  .text(text)
                  .replyMarkup(keyboard)
                  .build()));
    } catch (TelegramApiException | RuntimeException exception) {
      log.warn("Telegram message update failed");
      return false;
    }
  }

  public boolean delete(String chat, int id) {
    if (id <= 0) {
      return false;
    }
    try {
      return Boolean.TRUE.equals(client.execute(new DeleteMessage(chat, id)));
    } catch (TelegramApiException | RuntimeException exception) {
      log.warn("Telegram message deletion failed");
      return false;
    }
  }

  public boolean acknowledge(String callbackId, String text) {
    try {
      return !Boolean.FALSE.equals(
          client.execute(
              AnswerCallbackQuery.builder().callbackQueryId(callbackId).text(text).build()));
    } catch (TelegramApiException | RuntimeException exception) {
      log.warn("Telegram callback acknowledgment failed");
      return false;
    }
  }

  public void typing(String chat) {
    try {
      client.execute(new SendChatAction(chat, ActionType.TYPING.toString()));
    } catch (TelegramApiException | RuntimeException exception) {
      log.warn("Telegram typing status could not be sent");
    }
  }

  public static List<String> split(String text) {
    var chunks = new ArrayList<String>();
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
      if (end < text.length()
          && Character.isHighSurrogate(text.charAt(end - 1))
          && Character.isLowSurrogate(text.charAt(end))) {
        end--;
      }
      chunks.add(text.substring(start, end));
      start = end;
    }
    return chunks;
  }
}
