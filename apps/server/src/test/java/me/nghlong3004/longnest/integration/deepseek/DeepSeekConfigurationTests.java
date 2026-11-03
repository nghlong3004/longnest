package me.nghlong3004.longnest.integration.deepseek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.nghlong3004.longnest.configuration.LedgerConfiguration;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionMessageFormatter;
import me.nghlong3004.longnest.service.TransactionModelInterpreter;
import me.nghlong3004.longnest.service.TransactionQueryService;
import me.nghlong3004.longnest.service.TransactionService;
import me.nghlong3004.longnest.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class DeepSeekConfigurationTests {

  private static ValidatorFactory validators;

  @BeforeAll
  static void createValidator() {
    validators = Validation.buildDefaultValidatorFactory();
  }

  @AfterAll
  static void closeValidator() {
    validators.close();
  }

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(DeepSeekConfiguration.class, LedgerConfiguration.class)
          .withPropertyValues("longnest.ledger.time-zone=Asia/Ho_Chi_Minh")
          .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
          .withBean(Validator.class, () -> validators.getValidator())
          .withBean(TransactionService.class, () -> mock(TransactionService.class))
          .withBean(DraftService.class, () -> mock(DraftService.class))
          .withBean(TransactionQueryService.class, () -> mock(TransactionQueryService.class))
          .withBean(TransactionMessageFormatter.class, TransactionMessageFormatter::new);

  @Test
  void doesNotCreateAClientWhenDisabled() {
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).doesNotHaveBean(ChatClient.class);
    });
  }

  @Test
  void requiresAnApiKeyWhenEnabled() {
    runner
        .withBean(RestClient.Builder.class, RestClient::builder)
        .withPropertyValues(
            "longnest.deepseek.enabled=true",
            "longnest.deepseek.base-url=https://api.deepseek.com",
            "longnest.deepseek.model=deepseek-flash")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void requestsJsonModeWithResponseSchemaAndReferenceDateAndReturnsParsedReply() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var answer =
        """
        {"action":"CHAT","transaction":null,"reply":"Chào anh!"}
        """;
    var response =
        JsonMapper.builder()
            .build()
            .writeValueAsString(
                Map.of(
                    "id", "synthetic-completion",
                    "object", "chat.completion",
                    "created", 1793548800,
                    "model", "deepseek-flash",
                    "choices",
                        List.of(
                            Map.of(
                                "index",
                                0,
                                "message",
                                Map.of("role", "assistant", "content", answer),
                                "finish_reason",
                                "stop"))));
    server
        .expect(requestTo("https://api.deepseek.com/chat/completions"))
        .andExpect(header("Authorization", "Bearer synthetic-key"))
        .andExpect(jsonPath("$.model").value("deepseek-flash"))
        .andExpect(jsonPath("$.response_format.type").value("json_object"))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("ASK_CLARIFICATION")))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("LAST_MONTH")))
        .andExpect(
            jsonPath("$.messages[0].content")
                .value(containsString("Use the usable displayName for greetings")))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("Em chào Long")))
        .andExpect(jsonPath("$.messages[0].content").value(not(containsString("Chào bạn"))))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("\"query\"")))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("\"transaction\"")))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("\"timeOfDay\"")))
        .andExpect(jsonPath("$.messages[1].content").value(containsString("2026-11-02")))
        .andExpect(jsonPath("$.messages[2].content").value("Giới thiệu cách ghi thu chi"))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    runner
        .withUserConfiguration(ChatServiceImpl.class, TransactionModelInterpreter.class)
        .withBean(RestClient.Builder.class, () -> builder)
        .withPropertyValues(
            "longnest.deepseek.enabled=true",
            "longnest.deepseek.api-key=synthetic-key",
            "longnest.deepseek.base-url=https://api.deepseek.com",
            "longnest.deepseek.model=deepseek-flash")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(DeepSeekChatModel.class);
          var message =
              new ChatMessage(
                  Channel.TELEGRAM,
                  "123",
                  "123",
                  "1",
                  Instant.parse("2026-11-01T18:30:00Z"),
                  "Giới thiệu cách ghi thu chi");
          assertThat(context.getBean(ChatService.class).reply(message).text())
              .isEqualTo("Chào anh!");
          assertThat(context.getBean(DeepSeekProperties.class).toString())
              .doesNotContain("synthetic-key");
        });
    server.verify();
  }

  @Test
  void convertsNativeProviderHttpFailureToUnavailableErrorWithoutRetrying() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.deepseek.com/chat/completions"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    runner
        .withUserConfiguration(ChatServiceImpl.class, TransactionModelInterpreter.class)
        .withBean(RestClient.Builder.class, () -> builder)
        .withPropertyValues(
            "longnest.deepseek.enabled=true",
            "longnest.deepseek.api-key=synthetic-key",
            "longnest.deepseek.base-url=https://api.deepseek.com",
            "longnest.deepseek.model=deepseek-flash")
        .run(context -> {
          assertThat(context).hasNotFailed();
          var message =
              new ChatMessage(
                  Channel.TELEGRAM,
                  "123",
                  "123",
                  "1",
                  Instant.EPOCH,
                  "Giới thiệu cách ghi thu chi");
          assertThatThrownBy(() -> context.getBean(ChatService.class).reply(message))
              .isInstanceOfSatisfying(
                  ResourceException.class,
                  exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_UNAVAILABLE);
                    assertThat(exception.getCause()).isNotNull();
                  });
        });
    server.verify();
  }
}
