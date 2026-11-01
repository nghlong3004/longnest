package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramMessageHandlerTests {

  private final TelegramClient client = mock(TelegramClient.class);
  private final ChatService chatService = mock(ChatService.class);
  private final ResourceBundleMessageSource messages = messageSource();
  private final TelegramMessageHandler handler =
      new TelegramMessageHandler(
          client,
          new TelegramProperties("synthetic-token", 123L),
          chatService,
          new GlobalExceptionHandler(messages));

  @BeforeEach
  void stubChatbot() {
    when(chatService.reply(anyString())).thenAnswer(invocation -> "Bot: " + invocation.getArgument(0));
  }

  @Test
  void sendsOwnerTextToChatbotAndReturnsItsAnswerWithoutParsingMarkup() throws TelegramApiException {
    var text = "  Ăn trưa 45k ☕\n<b>nguyên văn</b> *test*  ";
    handler.consume(List.of(update(123L, 123L, "private", text)));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    verify(chatService).reply(text);
    assertThat(sent.getValue().getText()).isEqualTo("Bot: " + text);
    assertThat(sent.getValue().getChatId()).isEqualTo("123");
    assertThat(sent.getValue().getParseMode()).isNull();
  }

  @Test
  void sendsStartCommandToChatbotAsText() throws TelegramApiException {
    handler.consume(List.of(update(123L, 123L, "private", "/start")));
    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    assertThat(sent.getValue().getText()).isEqualTo("Bot: /start");
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

    verifyNoInteractions(client, chatService);
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
    assertThat(sent.getAllValues())
        .extracting(SendMessage::getText)
        .containsExactly("Bot: first", "Bot: second");
  }

  @Test
  void reportsModelFailureWithoutLeakingDetailsAndProcessesNextMessage() throws TelegramApiException {
    when(chatService.reply("first")).thenThrow(new IllegalStateException("sensitive provider detail"));

    handler.consume(
        List.of(
            update(123L, 123L, "private", "first"), update(123L, 123L, "private", "second")));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, times(2)).execute(sent.capture());
    assertThat(sent.getAllValues().getFirst().getText())
        .isEqualTo(
            messages.getMessage(
                ErrorCode.INTERNAL_ERROR.getMessageKey(), null, Locale.forLanguageTag("vi")))
        .isNotBlank()
        .doesNotContain("sensitive provider detail")
        .isNotEqualTo("first");
    assertThat(sent.getAllValues().getLast().getText()).isEqualTo("Bot: second");
  }

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void selectsTelegramErrorMessageUsingResourceExceptionCode(ErrorCode code)
      throws TelegramApiException {
    when(chatService.reply("failure"))
        .thenThrow(new ResourceException(code, new IllegalStateException("private details")));

    handler.consume(List.of(update(123L, 123L, "private", "failure")));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    assertThat(sent.getValue().getText())
        .isEqualTo(messages.getMessage(code.getMessageKey(), null, Locale.forLanguageTag("vi")))
        .doesNotContain("private details");
  }

  @Test
  void splitsLongAnswersWithoutLosingTextOrBreakingEmoji() throws TelegramApiException {
    var answer = "a".repeat(4095) + "😀" + "b".repeat(100);
    when(chatService.reply("long answer")).thenReturn(answer);

    handler.consume(List.of(update(123L, 123L, "private", "long answer")));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, times(2)).execute(sent.capture());
    assertThat(sent.getAllValues().stream().map(SendMessage::getText).collect(Collectors.joining()))
        .isEqualTo(answer);
    assertThat(sent.getAllValues())
        .allSatisfy(
            message -> {
              assertThat(message.getText().length()).isLessThanOrEqualTo(4096);
              assertThat(Character.isHighSurrogate(message.getText().charAt(message.getText().length() - 1)))
                  .isFalse();
            });
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

  private static ResourceBundleMessageSource messageSource() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    return messages;
  }
}
