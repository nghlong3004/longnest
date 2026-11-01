package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import me.nghlong3004.longnest.exception.GlobalExceptionHandler;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.dto.DraftCancellation;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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
        .thenAnswer(
            invocation -> ChatReply.text("Bot: " + invocation.<ChatMessage>getArgument(0).text()));
    doAnswer(
            invocation -> {
              invocation.<Runnable>getArgument(0).run();
              return typingTask;
            })
        .when(typingScheduler)
        .scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
  }

  @Test
  void sendsOwnerTextToChatbotAndReturnsItsAnswerWithoutParsingMarkup()
      throws TelegramApiException {
    var text = "  Ăn trưa 45k ☕\n<b>nguyên văn</b> *test*  ";
    handler.consume(List.of(update(123L, 123L, "private", text)));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    verify(chatService).reply(chatMessage(text));
    assertThat(sent.getValue().getText()).isEqualTo("Bot: " + text);
    assertThat(sent.getValue().getChatId()).isEqualTo("123");
    assertThat(sent.getValue().getParseMode()).isNull();
    assertThat(sent.getValue().getReplyParameters()).isNotNull();
    assertThat(sent.getValue().getReplyParameters().getMessageId()).isEqualTo(1);
  }

  @Test
  void eachReplyTargetsItsOwnIncomingMessage() throws TelegramApiException {
    var first = update(123L, 123L, "private", "first");
    first.getMessage().setMessageId(101);
    var second = update(123L, 123L, "private", "second");
    second.getMessage().setMessageId(102);

    handler.consume(List.of(first, second));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, times(2)).execute(sent.capture());
    assertThat(sent.getAllValues())
        .extracting(message -> message.getReplyParameters().getMessageId())
        .containsExactly(101, 102);
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
        .thenAnswer(invocation -> {
          verify(client).execute(any(SendChatAction.class));
          var renewal = ArgumentCaptor.forClass(Runnable.class);
          verify(typingScheduler)
              .scheduleWithFixedDelay(renewal.capture(), eq(Duration.ofSeconds(4)));
          renewal.getValue().run();
          return ChatReply.text("Xong rồi anh.");
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

    assertThatCode(() ->
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
  void reportsModelFailureWithoutLeakingDetailsAndProcessesNextMessage()
      throws TelegramApiException {
    when(chatService.reply(chatMessage("first")))
        .thenThrow(new IllegalStateException("sensitive provider detail"));

    handler.consume(
        List.of(update(123L, 123L, "private", "first"), update(123L, 123L, "private", "second")));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, times(2)).execute(sent.capture());
    assertThat(sent.getAllValues().getFirst().getText())
        .isEqualTo(
            messages.getMessage(
                ErrorCode.INTERNAL_ERROR.getMessageKey(), null, Locale.forLanguageTag("vi")))
        .isNotBlank()
        .doesNotContain("sensitive provider detail")
        .isNotEqualTo("first");
    assertThat(sent.getAllValues().getFirst().getReplyParameters().getMessageId()).isEqualTo(1);
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
    when(chatService.reply(chatMessage("long answer"))).thenReturn(ChatReply.text(answer));

    handler.consume(List.of(update(123L, 123L, "private", "long answer")));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client, times(2)).execute(sent.capture());
    assertThat(sent.getAllValues().stream().map(SendMessage::getText).collect(Collectors.joining()))
        .isEqualTo(answer);
    assertThat(sent.getAllValues())
        .allSatisfy(message -> {
          assertThat(message.getText().length()).isLessThanOrEqualTo(4096);
          assertThat(
                  Character.isHighSurrogate(
                      message.getText().charAt(message.getText().length() - 1)))
              .isFalse();
        });
  }

  @Test
  void clarificationHasACancelButtonAndPersistsTheSentQuestionId() throws TelegramApiException {
    var id = UUID.randomUUID();
    var reply = new ChatReply("Anh chi bao nhiêu?", id);
    when(chatService.reply(chatMessage("Ăn trưa"))).thenReturn(reply);
    when(client.execute(any(SendMessage.class)))
        .thenReturn(Message.builder().messageId(500).build());

    handler.consume(List.of(update(123L, 123L, "private", "Ăn trưa")));

    var sent = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(sent.capture());
    assertThat(sent.getValue().getReplyParameters().getMessageId()).isEqualTo(1);
    var keyboard = (InlineKeyboardMarkup) sent.getValue().getReplyMarkup();
    assertThat(keyboard.getKeyboard().getFirst().getFirst().getText()).isEqualTo("Hủy");
    assertThat(keyboard.getKeyboard().getFirst().getFirst().getCallbackData())
        .isEqualTo("cancel_draft:" + id);
    verify(chatService).registerQuestion(chatMessage("Ăn trưa"), reply, "500");
  }

  @Test
  void forwardsNativeReplyMetadataToTheChatService() {
    var incoming = update(123L, 123L, "private", "45k");
    incoming.getMessage().setReplyToMessage(Message.builder().messageId(500).build());
    handler.consume(List.of(incoming));

    var source = ArgumentCaptor.forClass(ChatMessage.class);
    verify(chatService).reply(source.capture());
    assertThat(source.getValue().replyToMessageId()).isEqualTo("500");
  }

  @Test
  void cancelButtonDeletesQuestionThenSendsOneConfirmationMessage() throws TelegramApiException {
    var id = UUID.randomUUID();
    when(chatService.cancelDraft(any(ChatMessage.class), eq(id)))
        .thenReturn(
            new DraftCancellation(
                DraftStatus.CANCELLED, "Em hủy khoản “Ăn trưa” rồi nhé.", true, "42"));

    handler.consume(List.of(callback(123L, 123L, "private", id)));

    var order = inOrder(chatService, client);
    var source = ArgumentCaptor.forClass(ChatMessage.class);
    order.verify(chatService).cancelDraft(source.capture(), eq(id));
    assertThat(source.getValue().externalUserId()).isEqualTo("123");
    assertThat(source.getValue().chatId()).isEqualTo("123");
    var toast = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
    order.verify(client).execute(toast.capture());
    assertThat(toast.getValue().getText()).isNull();
    assertThat(toast.getValue().getCallbackQueryId()).isEqualTo("callback-1");
    var deleted = ArgumentCaptor.forClass(DeleteMessage.class);
    order.verify(client).execute(deleted.capture());
    assertThat(deleted.getValue().getChatId()).isEqualTo("123");
    assertThat(deleted.getValue().getMessageId()).isEqualTo(500);
    var confirmation = ArgumentCaptor.forClass(SendMessage.class);
    order.verify(client).execute(confirmation.capture());
    assertThat(confirmation.getValue().getText()).isEqualTo("Em hủy khoản “Ăn trưa” rồi nhé.");
    assertThat(confirmation.getValue().getChatId()).isEqualTo("123");
    assertThat(confirmation.getValue().getReplyParameters().getMessageId()).isEqualTo(42);
    verifyNoInteractions(typingScheduler);
  }

  @Test
  void deleteFailureStillSendsTheCommittedCancellationConfirmation() throws TelegramApiException {
    var id = UUID.randomUUID();
    when(chatService.cancelDraft(any(ChatMessage.class), eq(id)))
        .thenReturn(
            new DraftCancellation(
                DraftStatus.CANCELLED, "Em hủy khoản “Ăn trưa” rồi nhé.", true, "42"));
    when(client.execute(any(DeleteMessage.class)))
        .thenThrow(new TelegramApiException("private deletion details"));

    assertThatCode(() -> handler.consume(List.of(callback(123L, 123L, "private", id))))
        .doesNotThrowAnyException();
    verify(client).execute(any(AnswerCallbackQuery.class));
    var confirmation = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(confirmation.capture());
    assertThat(confirmation.getValue().getText()).contains("Em hủy", "Ăn trưa");
  }

  @Test
  void repeatedCancelCallbacksDoNotSendAnotherConfirmation() throws TelegramApiException {
    var id = UUID.randomUUID();
    var text = "Em hủy khoản “Ăn trưa” rồi nhé.";
    when(chatService.cancelDraft(any(ChatMessage.class), eq(id)))
        .thenReturn(
            new DraftCancellation(DraftStatus.CANCELLED, text, true, "42"),
            new DraftCancellation(DraftStatus.CANCELLED, text, false, "42"));

    handler.consume(
        List.of(callback(123L, 123L, "private", id), callback(123L, 123L, "private", id)));

    var confirmation = ArgumentCaptor.forClass(SendMessage.class);
    verify(client).execute(confirmation.capture());
    assertThat(confirmation.getValue().getText()).isEqualTo(text);
    verify(client, times(2)).execute(any(DeleteMessage.class));
  }

  @Test
  void confirmedDraftButtonDoesNotDeleteTheQuestionOrCancelAFinancialTransaction()
      throws TelegramApiException {
    var id = UUID.randomUUID();
    when(chatService.cancelDraft(any(ChatMessage.class), eq(id)))
        .thenReturn(
            new DraftCancellation(DraftStatus.CONFIRMED, "Khoản này đã ghi rồi.", false, "42"));
    handler.consume(List.of(callback(123L, 123L, "private", id)));
    verify(client).execute(any(AnswerCallbackQuery.class));
    verify(client, never()).execute(any(DeleteMessage.class));
    verify(client, never()).execute(any(SendMessage.class));
  }

  @Test
  void callbackFailureUsesSafeLocalizedToastWithoutDeletingTheQuestion()
      throws TelegramApiException {
    var id = UUID.randomUUID();
    when(chatService.cancelDraft(any(ChatMessage.class), eq(id)))
        .thenThrow(
            new ResourceException(
                ErrorCode.DRAFT_NOT_FOUND, new IllegalStateException("private details")));
    handler.consume(List.of(callback(123L, 123L, "private", id)));
    var toast = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
    verify(client).execute(toast.capture());
    assertThat(toast.getValue().getText())
        .contains("Không tìm thấy")
        .doesNotContain("private details");
    verify(client, never()).execute(any(DeleteMessage.class));
  }

  @Test
  void unauthorizedCallbacksNeverReachTheDraftService() {
    var id = UUID.randomUUID();
    handler.consume(
        List.of(
            callback(456L, 456L, "private", id),
            callback(456L, 123L, "private", id),
            callback(123L, -100L, "group", id),
            callback(123L, 456L, "private", id)));
    verifyNoInteractions(client, chatService, typingScheduler);
  }

  private static Update callback(long sender, long chatId, String chatType, UUID draftId) {
    var question =
        Message.builder()
            .messageId(500)
            .date(1793548800)
            .chat(new Chat(chatId, chatType))
            .from(new User(999L, "Bot", true))
            .build();
    var query = new CallbackQuery();
    query.setId("callback-1");
    query.setFrom(new User(sender, "Synthetic", false));
    query.setMessage(question);
    query.setData("cancel_draft:" + draftId);
    var update = new Update();
    update.setCallbackQuery(query);
    return update;
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
