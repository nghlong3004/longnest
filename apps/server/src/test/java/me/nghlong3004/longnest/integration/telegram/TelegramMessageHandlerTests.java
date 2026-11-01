package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
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
  private final TaskScheduler typingScheduler = mock(TaskScheduler.class);
  private final ScheduledFuture<?> typingTask = mock(ScheduledFuture.class);
  private final ResourceBundleMessageSource messages = messageSource();
  private final TelegramMessageHandler handler =
      new TelegramMessageHandler(
          client,
          new TelegramProperties("synthetic-token", 123L),
          chatService,
          new GlobalExceptionHandler(messages),
          typingScheduler);

  @BeforeEach
  void stubChatbot() {
    when(chatService.reply(any(ChatMessage.class)))
        .thenAnswer(invocation -> "Bot: " + invocation.<ChatMessage>getArgument(0).text());
    doAnswer(
            invocation -> {
              invocation.<Runnable>getArgument(0).run();
              return typingTask;
            })
        .when(typingScheduler)
        .scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
  }

  @Test
  void sendsOwnerTextToChatbotAndReturnsItsAnswerWithoutParsingMarkup() throws TelegramApiException {
    var text = "  Ăn trưa 45k ☕\n<b>nguyên văn</b> *test*  ";
    handler.consume(List.of(update(123L, 123L, "private", text)));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    verify(chatService).reply(chatMessage(text));
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
  void showsTypingForTheOwnerWhilePreparingAReply() throws TelegramApiException {
    handler.consume(List.of(update(123L, 123L, "private", "Ăn trưa 45k")));

    var action = ArgumentCaptor.forClass(SendChatAction.class);
    verify(client).execute(action.capture());
    assertThat(action.getValue().getChatId()).isEqualTo("123");
    assertThat(action.getValue().getAction()).isEqualTo("typing");
  }

  @Test
  void renewsTypingWhileProcessingAndCancelsItBeforeTheFinalReply() throws TelegramApiException {
    when(chatService.reply(chatMessage("slow reply")))
        .thenAnswer(
            invocation -> {
              verify(client).execute(any(SendChatAction.class));
              var renewal = ArgumentCaptor.forClass(Runnable.class);
              verify(typingScheduler)
                  .scheduleWithFixedDelay(renewal.capture(), eq(Duration.ofSeconds(4)));
              renewal.getValue().run();
              return "Xong rồi anh.";
            });

    handler.consume(List.of(update(123L, 123L, "private", "slow reply")));

    var order = inOrder(client, typingTask);
    order.verify(client, times(2)).execute(any(SendChatAction.class));
    order.verify(typingTask).cancel(false);
    var sent = ArgumentCaptor.forClass(SendMessage.class);
    order.verify(client).execute(sent.capture());
    assertThat(sent.getValue().getText()).isEqualTo("Xong rồi anh.");
  }

  @Test
  void typingFailureDoesNotBlockTheReply() throws TelegramApiException {
    when(client.execute(any(SendChatAction.class)))
        .thenThrow(new TelegramApiException("private transport details"));

    handler.consume(List.of(update(123L, 123L, "private", "hello")));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    assertThat(sent.getValue().getText()).isEqualTo("Bot: hello");
    verify(typingTask).cancel(false);
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

    verifyNoInteractions(client, chatService, typingScheduler);
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
    when(chatService.reply(chatMessage("first")))
        .thenThrow(new IllegalStateException("sensitive provider detail"));

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
    verify(typingTask, times(2)).cancel(false);
  }

  @ParameterizedTest
  @EnumSource(ErrorCode.class)
  void selectsTelegramErrorMessageUsingResourceExceptionCode(ErrorCode code)
      throws TelegramApiException {
    when(chatService.reply(chatMessage("failure")))
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
    when(chatService.reply(chatMessage("long answer"))).thenReturn(answer);

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

  private static ChatMessage chatMessage(String text) {
    return new ChatMessage(
        Channel.TELEGRAM, "123", "123", "1", Instant.ofEpochSecond(1793548800), text);
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
