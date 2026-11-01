package me.nghlong3004.longnest.integration.deepseek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import me.nghlong3004.longnest.service.impl.ChatServiceImpl;
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

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(DeepSeekConfiguration.class)
          .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

  @Test
  void doesNotCreateAClientWhenDisabled() {
    runner.run(
        context -> {
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
  void requestsJsonModeWithResponseSchemaAndReturnsRawModelJson() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var answer =
        """
        {"action":"READY","transaction":{"type":"EXPENSE","amount":45000,
        "category":"FOOD","description":"Ăn trưa","location":null,"date":null,
        "timeOfDay":"NOON"},"reply":"Khoản chi 45.000đ cho ăn trưa — chưa lưu."}
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
                                "index", 0,
                                "message", Map.of("role", "assistant", "content", answer),
                                "finish_reason", "stop"))));
    server
        .expect(requestTo("https://api.deepseek.com/chat/completions"))
        .andExpect(header("Authorization", "Bearer synthetic-key"))
        .andExpect(jsonPath("$.model").value("deepseek-flash"))
        .andExpect(jsonPath("$.response_format.type").value("json_object"))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("ASK_CLARIFICATION")))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("\"transaction\"")))
        .andExpect(jsonPath("$.messages[0].content").value(containsString("\"timeOfDay\"")))
        .andExpect(jsonPath("$.messages[1].content").value("Xin chào"))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    runner
        .withBean(RestClient.Builder.class, () -> builder)
        .withPropertyValues(
            "longnest.deepseek.enabled=true",
            "longnest.deepseek.api-key=synthetic-key",
            "longnest.deepseek.base-url=https://api.deepseek.com",
            "longnest.deepseek.model=deepseek-flash")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(DeepSeekChatModel.class);
              assertThat(context.getBean(ChatClient.class).prompt().user("Xin chào").call().content())
                  .isEqualTo(answer);
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
        .withUserConfiguration(ChatServiceImpl.class)
        .withBean(RestClient.Builder.class, () -> builder)
        .withPropertyValues(
            "longnest.deepseek.enabled=true",
            "longnest.deepseek.api-key=synthetic-key",
            "longnest.deepseek.base-url=https://api.deepseek.com",
            "longnest.deepseek.model=deepseek-flash")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(() -> context.getBean(ChatService.class).reply("Xin chào"))
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
