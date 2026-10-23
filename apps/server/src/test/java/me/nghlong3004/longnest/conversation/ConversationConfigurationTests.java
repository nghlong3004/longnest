package me.nghlong3004.longnest.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Local context and fake-model checks: no provider request or external integration. */
class ConversationConfigurationTests {
  private static final Instant NOW = Instant.parse("2026-10-23T03:00:00Z");

  @Test
  void actualSdkSerializesNativeToolsAndReturnsWithoutAnotherProviderRequest() {
    var rest = RestClient.builder();
    var server = MockRestServiceServer.bindTo(rest).build();
    server.expect(requestTo("https://api.deepseek.com/chat/completions"))
        .andExpect(request -> {
          var body = JsonMapper.builder().build().readTree(
              ((MockClientHttpRequest) request).getBodyAsString());
          assertThat(body.path("model").asText()).isEqualTo("synthetic-model");
          assertThat(body.has("max_tokens")).isFalse();
          assertThat(body.has("max_completion_tokens")).isFalse();
          assertThat(body.has("max_output_tokens")).isFalse();
          assertThat(body.path("thinking").path("type").asText()).isEqualTo("disabled");
          assertThat(body.path("tools").size()).isEqualTo(8);
          assertThat(body.toString()).contains("list_transactions", "preview_update", "clarify")
              .doesNotContain("confirm_delete\"", "\"principal\"", "private:77");
          assertThat(body.has("response_format")).isFalse();
        }).andRespond(withSuccess("""
            {"id":"synthetic","object":"chat.completion","created":1,"model":"synthetic-model",
             "choices":[{"index":0,"finish_reason":"tool_calls","message":{"role":"assistant",
              "content":null,"tool_calls":[{"id":"call-1","type":"function","function":{
               "name":"clarify","arguments":"{\\"missing\\":\\"DATES\\"}"}}]}}],
             "usage":{"prompt_tokens":30,"completion_tokens":10,"total_tokens":40}}
            """, MediaType.APPLICATION_JSON));
    var api = DeepSeekApi.builder().apiKey("synthetic-key").baseUrl("https://api.deepseek.com")
        .restClientBuilder(rest).build();
    var model = ConversationConfiguration.buildModel(api, "synthetic-model");
    assertThat(model.getOptions().getMaxTokens()).isNull();
    var ledger = mock(LedgerClient.class);
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Ok<>(Optional.empty()));
    try (var executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())) {
      var assistant = new ConversationAssistant(model, new PersonalOwner("personal"), ledger,
          Clock.fixed(NOW, ZoneOffset.UTC), executor,
          new LedgerTools(ledger, mock(TransactionMapper.class)), JsonMapper.builder().build());
      assertThat(assistant.reply(new HostContext("personal", "event", NOW, NOW, "private:77"),
          "Tôi muốn xem khoản chi").messageKey()).isEqualTo("conversation.DATES");
      server.verify();
      verify(ledger).recoverOperation(any());
      verifyNoMoreInteractions(ledger);
    }
  }

  @Test
  void providerBindingRequiresLedgerProfileAndExplicitOptIn() {
    absent(true);
    absent(true, "ledger-init");
    absent(true, "smoke");
    absent(true, "ledger", "ledger-init");
    absent(true, "ledger", "smoke");
    absent(false, "ledger");
  }

  @Test
  void optedInLedgerRequiresDirectConfiguredProviderAndCreatesNoSession() {
    var runner = configured().withPropertyValues("longnest.deepseek.enabled=true",
        "longnest.deepseek.base-url=https://api.deepseek.com",
        "longnest.deepseek.model=deepseek-chat");
    runner.run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure()).hasStackTraceContaining(
          "Explicit DeepSeek key, direct base URL and model required");
    });
    runner.withPropertyValues("longnest.deepseek.api-key=synthetic-key",
        "longnest.deepseek.base-url=https://example.invalid").run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure()).hasStackTraceContaining(
              "Explicit DeepSeek key, direct base URL and model required");
        });
    runner.withPropertyValues("longnest.deepseek.api-key=synthetic-key")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(ConversationAssistant.class);
        });
  }

  @Test
  void nativeToolRequestRunsThroughChatClientButClarificationNeverWrites() {
    var model = mock(ChatModel.class);
    when(model.getOptions()).thenReturn(DeepSeekChatOptions.builder().build());
    var ledger = mock(LedgerClient.class);
    var owner = new PersonalOwner("personal");
    var response = new ChatResponse(List.of(new Generation(
        AssistantMessage.builder().content("").toolCalls(List.of(new AssistantMessage.ToolCall(
            "call-1", "function", "clarify", "{\"missing\":\"DATES\"}"))).build(),
        ChatGenerationMetadata.builder().finishReason("tool_calls").build())));
    when(model.call(any(Prompt.class))).thenReturn(response);
    when(ledger.recoverOperation(any())).thenReturn(new LedgerResult.Ok<>(Optional.empty()));
    try (var executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory())) {
      var assistant = new ConversationAssistant(model, owner, ledger,
          Clock.fixed(NOW, ZoneOffset.UTC), executor,
          new LedgerTools(ledger, mock(TransactionMapper.class)), JsonMapper.builder().build());
      var host = new HostContext("personal", "tg:123:9", NOW, NOW, "private:77");
      var reply = assistant.reply(host, "Cho tôi các khoản chi khi nào?");
      assertThat(reply.result()).isNull();
      assertThat(reply.messageKey()).isEqualTo("conversation.DATES");
      var prompt = ArgumentCaptor.forClass(Prompt.class);
      verify(model).call(prompt.capture());
      assertThat(prompt.getValue().getUserMessage().getText())
          .isEqualTo("Cho tôi các khoản chi khi nào?");
      assertThat(prompt.getValue().getOptions()).isInstanceOf(ToolCallingChatOptions.class);
      var options = (ToolCallingChatOptions) prompt.getValue().getOptions();
      assertThat(options.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
          .contains("clarify", "list_transactions");
      verify(ledger).recoverOperation(host);
      verify(ledger, never()).create(any(), any());
      verifyNoMoreInteractions(ledger);
    }
  }

  private static void absent(boolean enabled, String... profiles) {
    new ApplicationContextRunner().withUserConfiguration(ConversationConfiguration.class)
        .withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles))
        .withPropertyValues("longnest.deepseek.enabled=" + enabled)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(ConversationAssistant.class);
        });
  }

  private static ApplicationContextRunner configured() {
    return new ApplicationContextRunner().withUserConfiguration(ConversationConfiguration.class)
        .withPropertyValues("spring.profiles.active=ledger")
        .withBean(PersonalOwner.class, () -> new PersonalOwner("personal"))
        .withBean(LedgerClient.class, () -> mock(LedgerClient.class))
        .withBean(TransactionMapper.class, () -> mock(TransactionMapper.class))
        .withBean(Clock.class, () -> Clock.fixed(NOW, ZoneOffset.UTC))
        .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
        .withBean("applicationTaskExecutor", AsyncTaskExecutor.class,
            () -> mock(AsyncTaskExecutor.class));
  }
}
