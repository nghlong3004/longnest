package me.nghlong3004.longnest.integration.telegram;

import java.time.Clock;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.TransactionPageRequest;
import me.nghlong3004.longnest.model.enums.Channel;
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
public class TelegramQueryCallbackHandler implements TelegramCallbackHandler {
  private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
  private final TelegramMessageSender sender;
  private final ChatService chatService;
  private final GlobalExceptionHandler exceptionHandler;
  private final TelegramTransactionQueryCodec queryCodec;
  private final TelegramTransactionActions transactionActions;
  private final Clock ledgerClock;

  @Override
  public boolean supports(String data) {
    return data.startsWith(TelegramTransactionQueryCodec.PREFIX);
  }

  @Override
  public void handleCallback(CallbackQuery callback, Message message) {
    TransactionPageRequest request;
    try {
      request = queryCodec.decode(callback.getData());
    } catch (IllegalArgumentException exception) {
      sender.acknowledge(callback.getId(), "Nút này không hợp lệ.");
      return;
    }
    var chat = message.getChatId().toString();
    if (transactionActions.isRevokedPage(chat, callback.getData(), message.getMessageId())) {
      sender.acknowledge(callback.getId(), "Trang cũ đã thay đổi. Hãy mở lại danh sách cập nhật.");
      return;
    }
    // Stop Telegram's button spinner before accessing the database.
    sender.acknowledge(callback.getId(), null);
    var source =
        new ChatMessage(
            Channel.TELEGRAM,
            callback.getFrom().getId().toString(),
            chat,
            Integer.toString(request.messageId()),
            ledgerClock.instant(),
            "Xem thêm");
    ChatReply reply;
    try {
      reply = chatService.nextPage(source, request);
    } catch (RuntimeException exception) {
      reply = ChatReply.text(exceptionHandler.handle(exception, VIETNAMESE));
    }
    try {
      if (reply.transactions() != null) {
        transactionActions.sendPage(chat, request.messageId(), reply);
      } else {
        sender.sendText(
            chat,
            request.messageId(),
            reply.text(),
            null,
            TelegramMessageSender.KeyboardPlacement.FIRST);
      }
    } catch (TelegramApiException exception) {
      log.warn("Transaction page could not be sent");
    }
  }
}
