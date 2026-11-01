package me.nghlong3004.longnest.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.TransactionService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.transaction.TransactionSystemException;
import tools.jackson.databind.json.JsonMapper;

class ChatServiceImplTests {

  private static ValidatorFactory validators;
  private final TransactionService transactions = mock(TransactionService.class);

  @BeforeAll
  static void createValidator() {
    validators = Validation.buildDefaultValidatorFactory();
  }

  @AfterAll
  static void closeValidator() {
    validators.close();
  }

  @Test
  void sendsLiteralUserTextWithReferenceDateAndReturnsParsedReply() {
    var model = modelReturning("""
        {"action":"CHAT","transaction":null,"reply":"Chào anh!"}
        """);
    var service = service(model);
    var text = "Xin chào {name}, đây là JSON: {\"amount\":45000}";

    assertThat(service.reply(message(text))).isEqualTo("Chào anh!");

    var prompt = ArgumentCaptor.forClass(Prompt.class);
    verify(model).call(prompt.capture());
    assertThat(prompt.getValue().getUserMessage().getText()).isEqualTo(text);
    assertThat(prompt.getValue().getSystemMessage().getText()).contains("2026-11-02", "Asia/Ho_Chi_Minh");
    verifyNoInteractions(transactions);
  }

  @ParameterizedTest
  @MethodSource("invalidResponses")
  void rejectsInvalidModelDataBeforeAnyDatabaseCall(String answer) {
    var service = service(modelReturning(answer));

    assertThatThrownBy(() -> service.reply(message("Ăn trưa 45k")))
        .isInstanceOfSatisfying(ResourceException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_INVALID_RESPONSE));
    verifyNoInteractions(transactions);
  }

  static Stream<String> invalidResponses() {
    var ready = """
        {"action":"READY","transaction":{"type":"EXPENSE","amount":45000,
        "category":null,"description":null,"location":null,"date":null,"timeOfDay":null},
        "reply":"Khoản chi 45.000đ."}
        """;
    var chat = "{\"action\":\"CHAT\",\"transaction\":null,\"reply\":\"Chào anh!\"}";
    return Stream.of(
        "not JSON", "null", "[]", "```json\n" + chat + "\n```", chat + " " + chat,
        chat.replace("\"CHAT\"", "\"UNKNOWN\""), chat.replace("\"CHAT\"", "null"),
        chat.replace("Chào anh!", " "), chat.replace("\"reply\"", "\"userId\""),
        ready.replace("\"EXPENSE\"", "null"), ready.replace("\"EXPENSE\"", "\"TRANSFER\""),
        ready.replace("45000", "null"), ready.replace("45000", "0"), ready.replace("45000", "-1"),
        ready.replace("45000", "0.001"), ready.replace("45000", "100000000000000000"),
        ready.replace("\"category\":null", "\"category\":\"FOOD\"").replace("EXPENSE", "INCOME"),
        ready.replace("\"category\":null", "\"category\":\"UNKNOWN\""),
        ready.replace("\"date\":null", "\"date\":\"2026-02-30\""),
        ready.replace("\"timeOfDay\":null", "\"timeOfDay\":\"MIDNIGHT\""),
        ready.replace("\"action\":\"READY\"", "\"action\":0"),
        ready.replace("\"type\":\"EXPENSE\"", "\"type\":1"),
        ready.replace("\"category\":null", "\"category\":0"),
        ready.replace("\"timeOfDay\":null", "\"timeOfDay\":0"),
        ready.replace("READY", "CHAT"),
        chat.replace("\"action\":\"CHAT\"", "\"action\":\"READY\",\"action\":\"CHAT\""),
        ready.replace("\"amount\":45000", "\"amount\":1,\"amount\":45000"),
        "{\"action\":\"READY\",\"transaction\":null,\"reply\":\"Xong\"}");
  }

  @Test
  void preservesEchoWhenDeepSeekIsDisabled() {
    var service = service(new StaticListableBeanFactory());
    assertThat(service.reply(message("  Xin chào ☕  "))).isEqualTo("  Xin chào ☕  ");
    verifyNoInteractions(transactions);
  }

  @Test
  void rejectsEmptyModelOutputSoTelegramCanReportFailure() {
    var service = service(modelReturning("   "));

    assertThatThrownBy(() -> service.reply(message("Xin chào")))
        .isInstanceOfSatisfying(ResourceException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_EMPTY_RESPONSE));
    verifyNoInteractions(transactions);
  }

  @Test
  void convertsProviderConnectionFailureToResourceExceptionAndPreservesCause() {
    var model = modelReturning("unused");
    var failure = new ResourceAccessException("sensitive provider details");
    when(model.call(any(Prompt.class))).thenThrow(failure);
    var service = service(model);

    assertThatThrownBy(() -> service.reply(message("Xin chào")))
        .isInstanceOfSatisfying(ResourceException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_UNAVAILABLE);
          assertThat(exception.getCause()).isSameAs(failure);
          assertThat(exception.getMessage()).doesNotContain("sensitive provider details");
        });
    verifyNoInteractions(transactions);
  }

  @Test
  void doesNotDisguiseProgrammingErrorsAsProviderFailures() {
    var model = modelReturning("unused");
    var failure = new NullPointerException("synthetic bug");
    when(model.call(any(Prompt.class))).thenThrow(failure);
    var service = service(model);

    assertThatThrownBy(() -> service.reply(message("Xin chào"))).isSameAs(failure);
  }

  @Test
  void rejectsMissingTrustedMetadataWithoutCallingProvider() {
    var model = modelReturning("unused");
    var service = service(model);
    var invalid = new ChatMessage(Channel.TELEGRAM, null, "123", "1", Instant.EPOCH, "Ăn trưa 45k");

    assertThatThrownBy(() -> service.reply(invalid)).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(transactions);
    verify(model, never()).call(any(Prompt.class));
  }

  @Test
  void reportsCommitFailureWithoutReturningTheModelsConfirmation() {
    var model = modelReturning("""
        {"action":"READY","transaction":{"type":"EXPENSE","amount":45000,
        "category":null,"description":null,"location":null,"date":null,"timeOfDay":null},
        "reply":"Đã lưu rồi."}
        """);
    var failure = new TransactionSystemException("private database details");
    when(transactions.record(any(), any())).thenThrow(failure);
    var service = service(model);

    assertThatThrownBy(() -> service.reply(message("Ăn trưa 45k")))
        .isInstanceOfSatisfying(ResourceException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TRANSACTION_SAVE_FAILED);
          assertThat(exception.getCause()).isSameAs(failure);
        });
  }

  private ChatService service(ChatModel model) {
    return service(new StaticListableBeanFactory(Map.of("chatClient", ChatClient.create(model))));
  }

  private ChatService service(StaticListableBeanFactory clients) {
    return new ChatServiceImpl(clients.getBeanProvider(ChatClient.class), JsonMapper.builder().build(),
        validators.getValidator(), transactions, ZoneId.of("Asia/Ho_Chi_Minh"));
  }

  private static ChatModel modelReturning(String answer) {
    var model = mock(ChatModel.class);
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(answer)))));
    return model;
  }

  private static ChatMessage message(String text) {
    return new ChatMessage(Channel.TELEGRAM, "123", "123", "1",
        Instant.parse("2026-11-01T18:30:00Z"), text);
  }
}
