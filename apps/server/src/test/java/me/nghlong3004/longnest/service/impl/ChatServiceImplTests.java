package me.nghlong3004.longnest.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import org.junit.jupiter.api.Test;
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

class ChatServiceImplTests {

  @Test
  void sendsLiteralUserTextToModelAndReturnsItsAnswer() {
    var model = mock(ChatModel.class);
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Chào anh!")))));
    var clients = new StaticListableBeanFactory(Map.of("chatClient", ChatClient.create(model)));
    ChatService service = new ChatServiceImpl(clients.getBeanProvider(ChatClient.class));
    var text = "Xin chào {name}, đây là JSON: {\"amount\":45000}";

    assertThat(service.reply(text)).isEqualTo("Chào anh!");

    var prompt = ArgumentCaptor.forClass(Prompt.class);
    verify(model).call(prompt.capture());
    assertThat(prompt.getValue().getUserMessage().getText()).isEqualTo(text);
  }

  @Test
  void preservesEchoWhenDeepSeekIsDisabled() {
    var clients = new StaticListableBeanFactory();
    ChatService service = new ChatServiceImpl(clients.getBeanProvider(ChatClient.class));
    assertThat(service.reply("  Xin chào ☕  ")).isEqualTo("  Xin chào ☕  ");
  }

  @Test
  void rejectsEmptyModelOutputSoTelegramCanReportFailure() {
    var model = mock(ChatModel.class);
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("   ")))));
    var clients = new StaticListableBeanFactory(Map.of("chatClient", ChatClient.create(model)));
    ChatService service = new ChatServiceImpl(clients.getBeanProvider(ChatClient.class));

    assertThatThrownBy(() -> service.reply("Xin chào"))
        .isInstanceOfSatisfying(
            ResourceException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_EMPTY_RESPONSE));
  }

  @Test
  void convertsProviderConnectionFailureToResourceExceptionAndPreservesCause() {
    var model = mock(ChatModel.class);
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
    var failure = new ResourceAccessException("sensitive provider details");
    when(model.call(any(Prompt.class))).thenThrow(failure);
    var clients = new StaticListableBeanFactory(Map.of("chatClient", ChatClient.create(model)));
    ChatService service = new ChatServiceImpl(clients.getBeanProvider(ChatClient.class));

    assertThatThrownBy(() -> service.reply("Xin chào"))
        .isInstanceOfSatisfying(
            ResourceException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_UNAVAILABLE);
              assertThat(exception.getCause()).isSameAs(failure);
              assertThat(exception.getMessage()).doesNotContain("sensitive provider details");
            });
  }

  @Test
  void doesNotDisguiseProgrammingErrorsAsProviderFailures() {
    var model = mock(ChatModel.class);
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
    var failure = new NullPointerException("synthetic bug");
    when(model.call(any(Prompt.class))).thenThrow(failure);
    var clients = new StaticListableBeanFactory(Map.of("chatClient", ChatClient.create(model)));
    ChatService service = new ChatServiceImpl(clients.getBeanProvider(ChatClient.class));

    assertThatThrownBy(() -> service.reply("Xin chào")).isSameAs(failure);
  }
}
