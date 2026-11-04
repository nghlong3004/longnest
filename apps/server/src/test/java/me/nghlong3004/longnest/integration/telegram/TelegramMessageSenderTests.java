package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramMessageSenderTests {
  private final TelegramClient client = mock(TelegramClient.class);
  private final TelegramMessageSender sender = new TelegramMessageSender(client);

  @ParameterizedTest
  @EnumSource(TelegramMessageSender.KeyboardPlacement.class)
  void preservesUnicodeAndPlacesControlsOnTheRequestedChunk(
      TelegramMessageSender.KeyboardPlacement placement) throws TelegramApiException {
    var text = "a".repeat(4095) + "😀" + "b".repeat(4096);
    var keyboard =
        new InlineKeyboardMarkup(
            List.of(
                new InlineKeyboardRow(
                    InlineKeyboardButton.builder().text("Save").callbackData("save").build())));

    var delivered = sender.sendText("123", 42, text, keyboard, placement);

    assertThat(delivered).hasSize(3).containsOnlyNulls();
    var requests = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, times(3)).execute(requests.capture());
    assertThat(
            requests.getAllValues().stream().map(SendMessage::getText).reduce("", String::concat))
        .isEqualTo(text);
    for (int index = 0; index < 3; index++) {
      var request = requests.getAllValues().get(index);
      assertThat(request.getText()).hasSizeLessThanOrEqualTo(4096);
      assertThat(
              Character.isHighSurrogate(request.getText().charAt(request.getText().length() - 1)))
          .isFalse();
      assertThat(request.getReplyParameters().getMessageId()).isEqualTo(42);
      assertThat(request.getReplyParameters().getAllowSendingWithoutReply()).isTrue();
      assertThat(request.getParseMode()).isNull();
      boolean controls =
          index == (placement == TelegramMessageSender.KeyboardPlacement.FIRST ? 0 : 2);
      assertThat(request.getReplyMarkup()).isEqualTo(controls ? keyboard : null);
    }
  }

  @Test
  void stopsAtAFailedChunkAndPropagatesDeliveryFailure() throws TelegramApiException {
    when(client.execute(any(SendMessage.class)))
        .thenThrow(new TelegramApiException("private delivery details"));
    assertThatThrownBy(
            () ->
                sender.sendText(
                    "123",
                    42,
                    "x".repeat(5000),
                    null,
                    TelegramMessageSender.KeyboardPlacement.LAST))
        .isInstanceOf(TelegramApiException.class);
    verify(client).execute(any(SendMessage.class));
  }

  @Test
  void convertsTransportFailuresIntoFalseOutcomesAndTypingRemainsBestEffort()
      throws TelegramApiException {
    when(client.execute(any(EditMessageText.class)))
        .thenThrow(new TelegramApiException("private update details"));
    when(client.execute(any(DeleteMessage.class)))
        .thenThrow(new TelegramApiException("private delete details"));
    when(client.execute(any(AnswerCallbackQuery.class)))
        .thenThrow(new TelegramApiException("private callback details"));
    when(client.execute(any(SendChatAction.class)))
        .thenThrow(new IllegalStateException("private typing details"));

    assertThat(sender.edit("123", 1, "Updated", null)).isFalse();
    assertThat(sender.delete("123", 1)).isFalse();
    assertThat(sender.acknowledge("callback", "Updated")).isFalse();
    assertThatCode(() -> sender.typing("123")).doesNotThrowAnyException();
  }

  @Test
  void reportsTelegramRefusalToDelete() throws TelegramApiException {
    when(client.execute(any(DeleteMessage.class))).thenReturn(false);
    assertThat(sender.delete("123", 1)).isFalse();
  }
}
