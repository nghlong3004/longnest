package me.nghlong3004.longnest.conversation;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** No provider binding exists on default, init or smoke paths. */
@Configuration(proxyBeanMethods = false)
@Profile("ledger & !ledger-init & !smoke")
@ConditionalOnProperty(prefix = "longnest.deepseek", name = "enabled", havingValue = "true")
public class ConversationConfiguration {
  @Bean
  ConversationAssistant conversationAssistant(PersonalOwner owner, LedgerClient ledger,
      TransactionMapper mapper, Clock clock, JsonMapper json,
      @Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor,
      @Value("${longnest.deepseek.api-key:}") String key,
      @Value("${longnest.deepseek.base-url:}") String baseUrl,
      @Value("${longnest.deepseek.model:}") String name) {
    if (key.isBlank() || !baseUrl.equals("https://api.deepseek.com")
        || !name.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}")) {
      throw new IllegalArgumentException("Explicit DeepSeek key, direct base URL and model required");
    }
    var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    var requests = new JdkClientHttpRequestFactory(http);
    requests.setReadTimeout(Duration.ofSeconds(8));
    var api = DeepSeekApi.builder().baseUrl(baseUrl).apiKey(key)
        .restClientBuilder(RestClient.builder().requestFactory(requests))
        .responseErrorHandler(response -> {
          if (response.getStatusCode().isError()) {
            throw new IllegalStateException("Conversation provider unavailable");
          }
          return false;
        }).build();
    return new ConversationAssistant(buildModel(api, name), owner, ledger, clock, executor,
        new LedgerTools(ledger, mapper), json);
  }

  static DeepSeekChatModel buildModel(DeepSeekApi api, String name) {
    var options = DeepSeekChatOptions.builder().model(name).disableThinking().build();
    return DeepSeekChatModel.builder().deepSeekApi(api).options(options)
        .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0))).build();
  }
}
