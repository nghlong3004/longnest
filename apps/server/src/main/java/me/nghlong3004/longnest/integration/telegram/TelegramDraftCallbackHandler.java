package me.nghlong3004.longnest.integration.telegram;

import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.model.dto.DraftCancellation;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.service.ChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramDraftCallbackHandler implements TelegramCallbackHandler {
  static final String PREFIX = "cancel_draft:";
  private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
  private final TelegramMessageSender sender;
  private final ChatService chatService;
  private final GlobalExceptionHandler exceptionHandler;
  private final TelegramMessageMapper mapper;

  @Override
  public boolean supports(String data) {
    return data.startsWith(PREFIX);
  }

  @Override
  public void handleCallback(CallbackQuery callback, Message message) {
    UUID draftId;
    try {
      draftId = UUID.fromString(callback.getData().substring(PREFIX.length()));
    } catch (IllegalArgumentException exception) {
      sender.acknowledge(callback.getId(), "Nút này không hợp lệ.");
      return;
    }
    try {
      var source = mapper.toChatMessage(message, callback.getFrom(), "/cancel");
      var result = chatService.cancelDraft(source, draftId);
      if (result.status() == DraftStatus.CANCELLED) {
        finishCancellation(callback.getId(), message, result);
      } else {
        sender.acknowledge(callback.getId(), result.reply());
      }
    } catch (RuntimeException exception) {
      sender.acknowledge(callback.getId(), exceptionHandler.handle(exception, VIETNAMESE));
    }
  }

  private void finishCancellation(String callbackId, Message question, DraftCancellation result) {
    sender.acknowledge(callbackId, result.changed() ? null : "Khoản này đã hủy rồi.");
    var chat = question.getChatId().toString();
    sender.delete(chat, question.getMessageId());
    if (result.changed()) {
      try {
        sender.sendText(
            chat,
            Integer.parseInt(result.originalMessageId()),
            result.reply(),
            null,
            TelegramMessageSender.KeyboardPlacement.FIRST);
      } catch (TelegramApiException exception) {
        log.warn("Cancellation confirmation could not be sent");
      }
    }
  }
}
