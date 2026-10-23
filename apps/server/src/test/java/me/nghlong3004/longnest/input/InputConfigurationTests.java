package me.nghlong3004.longnest.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.api.ResponseFormat;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.json.JsonMapper;

/** Bean construction only: no SDK calls, credentials or network. */
class InputConfigurationTests {
  private final ApplicationContextRunner contexts = new ApplicationContextRunner()
      .withUserConfiguration(InputConfiguration.class)
      .withBean("applicationTaskExecutor", AsyncTaskExecutor.class,
          () -> new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory()))
      .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

  @Test
  void ledgerDefaultsToRulesWithoutProviderConfig() {
    contexts.withPropertyValues("spring.profiles.active=ledger").run(context -> {
      assertThat(context).hasSingleBean(InputInterpreter.class);
      assertThat(context.getBean(InputInterpreter.class)).isInstanceOf(TransactionInterpreter.class);
      assertThat(context).doesNotHaveBean(ChatModel.class);
    });
  }

  @Test
  void initAndSmokeCannotConstructFallbackEvenIfEnabledIsInherited() {
    for (String profile : new String[] {"ledger,ledger-init", "ledger,smoke", "smoke"}) {
      contexts.withPropertyValues("spring.profiles.active=" + profile,
          "longnest.deepseek.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(InputInterpreter.class);
          });
    }
  }

  @Test
  void enabledRequiresExplicitConfigButConstructionMakesNoProviderCall() {
    contexts.withPropertyValues("spring.profiles.active=ledger", "longnest.deepseek.enabled=true")
        .run(context -> assertThat(context).hasFailed());
    contexts.withPropertyValues("spring.profiles.active=ledger", "longnest.deepseek.enabled=true",
        "longnest.deepseek.api-key=synthetic-unused", "longnest.deepseek.model=synthetic-model",
        "longnest.deepseek.base-url=https://api.deepseek.com").run(context -> {
          assertThat(context).hasNotFailed();
          var interpreter = context.getBean(InputInterpreter.class);
          assertThat(interpreter).isInstanceOf(DeepSeekInterpreter.class);
          var model = (DeepSeekChatModel) ReflectionTestUtils.getField(interpreter, "model");
          var options = model.getOptions();
          assertThat(options.getModel()).isEqualTo("synthetic-model");
          assertThat(options.getMaxTokens()).isEqualTo(512);
          assertThat(options.getThinking()).isEqualTo(DeepSeekApi.ChatCompletionRequest.Thinking.DISABLED);
          assertThat(options.getResponseFormat().getType()).isEqualTo(ResponseFormat.Type.JSON_OBJECT);
          assertThat(options.getToolCallbacks()).isNullOrEmpty();
          assertThat(options.getTools()).isNullOrEmpty();
          var attempts = new AtomicInteger();
          assertThatThrownBy(() -> model.retryTemplate.invoke(() -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("synthetic unavailable");
          })).isInstanceOf(RuntimeException.class);
          assertThat(attempts).hasValue(1);
        });
  }

  @Test
  void redirectsAndArbitraryEndpointsCannotBeSelected() {
    contexts.withPropertyValues("spring.profiles.active=ledger", "longnest.deepseek.enabled=true",
        "longnest.deepseek.api-key=synthetic-unused", "longnest.deepseek.model=synthetic-model",
        "longnest.deepseek.base-url=http://localhost:1234").run(context ->
            assertThat(context).hasFailed());
  }

  @Test
  void actualSdkEncodesBoundedJsonRequestAndDecodesFakeResponseWithoutNetwork() {
    var rest = RestClient.builder();
    var server = MockRestServiceServer.bindTo(rest).build();
    server.expect(requestTo("https://api.deepseek.com/chat/completions"))
        .andExpect(request -> {
          var wire = JsonMapper.builder().build()
              .readTree(((MockClientHttpRequest) request).getBodyAsString());
          assertThat(wire.path("response_format").path("type").asString()).isEqualTo("json_object");
          assertThat(wire.path("thinking").path("type").asString()).isEqualTo("disabled");
          assertThat(wire.path("max_tokens").asInt()).isEqualTo(512);
          assertThat(wire.path("model").asString()).isEqualTo("synthetic-model");
          assertThat(wire.hasNonNull("tools")).isFalse();
        }).andRespond(withSuccess("""
            {"id":"fake","object":"chat.completion","created":123,"model":"synthetic-model",
             "choices":[{"index":0,"message":{"role":"assistant","content":"{}"},
               "finish_reason":"stop"}],
             "usage":{"prompt_tokens":10,"completion_tokens":1,"total_tokens":11}}
            """, MediaType.APPLICATION_JSON));
    var api = DeepSeekApi.builder().apiKey("synthetic-unused")
        .baseUrl("https://api.deepseek.com").restClientBuilder(rest).build();
    var response = InputConfiguration.buildModel(api, "synthetic-model")
        .call(new Prompt("synthetic json fixture only"));
    assertThat(response.getResult().getOutput().getText()).isEqualTo("{}");
    server.verify();
  }
}
