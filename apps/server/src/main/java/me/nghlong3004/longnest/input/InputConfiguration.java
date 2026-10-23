package me.nghlong3004.longnest.input;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.api.ResponseFormat;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Explicit opt-in factory: no provider client exists on default, init or smoke paths. */
@Configuration(proxyBeanMethods = false)
@Profile("ledger & !ledger-init & !smoke")
public class InputConfiguration {
  @Bean
  @ConditionalOnProperty(prefix = "longnest.deepseek", name = "enabled",
      havingValue = "false", matchIfMissing = true)
  InputInterpreter rulesInterpreter() {
    return new TransactionInterpreter();
  }

  @Bean
  @ConditionalOnProperty(prefix = "longnest.deepseek", name = "enabled", havingValue = "true")
  InputInterpreter deepSeekInterpreter(JsonMapper json,
      @Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor,
      @Value("${longnest.deepseek.api-key:}") String key,
      @Value("${longnest.deepseek.base-url:}") String baseUrl,
      @Value("${longnest.deepseek.model:}") String modelName) {
    if (key.isBlank() || !baseUrl.equals("https://api.deepseek.com")
        || !modelName.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}")) {
      throw new IllegalArgumentException("Explicit DeepSeek key, approved direct base URL and model required");
    }
    var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    var requests = new JdkClientHttpRequestFactory(http);
    requests.setReadTimeout(Duration.ofSeconds(8));
    var rest = RestClient.builder().requestFactory(requests);
    var api = DeepSeekApi.builder().baseUrl(baseUrl).apiKey(key).restClientBuilder(rest)
        .responseErrorHandler(response -> {
          if (response.getStatusCode().isError()) {
            throw new IllegalStateException("Interpretation provider unavailable");
          }
          return false;
        }).build();
    var model = buildModel(api, modelName);
    var strict = json.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    return new DeepSeekInterpreter(new TransactionInterpreter(), model, strict, executor);
  }

  static DeepSeekChatModel buildModel(DeepSeekApi api, String modelName) {
    var options = DeepSeekChatOptions.builder().model(modelName).disableThinking()
        .maxTokens(512).responseFormat(ResponseFormat.builder()
            .type(ResponseFormat.Type.JSON_OBJECT).build()).build();
    return DeepSeekChatModel.builder().deepSeekApi(api).options(options)
        .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0))).build();
  }
}
