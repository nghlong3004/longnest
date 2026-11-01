package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramEchoHandlerTests {

  private final TelegramClient client = mock(TelegramClient.class);
  private final TelegramEchoHandler handler =
      new TelegramEchoHandler(client, new TelegramProperties("synthetic-token", 123L));

  @Test
  void echoesPrivateOwnerTextExactlyWithoutParsingMarkup() throws TelegramApiException {
    var text = "  Ăn trưa 45k ☕\n<b>nguyên văn</b> *test*  ";
    handler.consume(List.of(update(123L, 123L, "private", text)));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    assertThat(sent.getValue().getText()).isEqualTo(text);
    assertThat(sent.getValue().getChatId()).isEqualTo("123");
    assertThat(sent.getValue().getParseMode()).isNull();
  }

  @Test
  void echoesStartCommandAsText() throws TelegramApiException {
    handler.consume(List.of(update(123L, 123L, "private", "/start")));
    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    assertThat(sent.getValue().getText()).isEqualTo("/start");
  }

  @Test
  void ignoresOtherUsersGroupsBotSendersMediaAndNonMessageUpdates() {
    var missingSender = update(123L, 123L, "private", "missing sender");
    missingSender.getMessage().setFrom(null);
    var botSender = update(123L, 123L, "private", "bot");
    botSender.getMessage().getFrom().setIsBot(true);

    handler.consume(
        List.of(
            update(456L, 456L, "private", "unauthorized"),
            update(123L, -100L, "group", "group"),
            update(456L, 123L, "private", "spoofed chat"),
            update(123L, 456L, "private", "wrong private chat"),
            update(123L, 123L, "private", null),
            missingSender,
            botSender,
            new Update()));

    verifyNoInteractions(client);
  }

  @Test
  void continuesWithNextMessageAfterSendFailure() throws TelegramApiException {
    doThrow(new TelegramApiException("synthetic failure"))
        .doReturn(new Message())
        .when(client)
        .execute(any(SendMessage.class));

    assertThatCode(
            () ->
                handler.consume(
                    List.of(
                        update(123L, 123L, "private", "first"),
                        update(123L, 123L, "private", "second"))))
        .doesNotThrowAnyException();
    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, times(2)).execute(sent.capture());
    assertThat(sent.getAllValues()).extracting(SendMessage::getText).containsExactly("first", "second");
  }

  private static Update update(long senderId, long chatId, String chatType, String text) {
    var message =
        Message.builder()
            .messageId(1)
            .date(1793548800)
            .from(new User(senderId, "Synthetic", false))
            .chat(new Chat(chatId, chatType))
            .text(text)
            .build();
    var update = new Update();
    update.setUpdateId(1);
    update.setMessage(message);
    return update;
  }
}
