package me.nghlong3004.longnest.integration.deepseek;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.model.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.api.ResponseFormat;
import org.springframework.ai.template.NoOpTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "longnest.deepseek", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DeepSeekProperties.class)
public class DeepSeekConfiguration {

  @Bean
  DeepSeekApi deepSeekApi(DeepSeekProperties properties, RestClient.Builder restClientBuilder) {
    return DeepSeekApi.builder()
        .baseUrl(properties.baseUrl())
        .apiKey(properties.apiKey())
        .restClientBuilder(restClientBuilder)
        .build();
  }

  @Bean
  DeepSeekChatModel deepSeekChatModel(DeepSeekApi api, DeepSeekProperties properties) {
    return DeepSeekChatModel.builder()
        .deepSeekApi(api)
        .options(
            DeepSeekChatOptions.builder()
                .model(properties.model())
                .disableThinking()
                .responseFormat(
                    ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                .build())
        .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0)))
        .build();
  }

  @Bean
  ChatClient deepSeekChatClient(
      DeepSeekChatModel model,
      JsonMapper jsonMapper,
      @Value("classpath:prompts/transaction-chat-system.txt") Resource systemPrompt)
      throws IOException {
    var format = new BeanOutputConverter<>(ChatResponse.class, jsonMapper).getFormat();
    log.info("DeepSeek structured chat enabled");
    return ChatClient.builder(model)
        .defaultTemplateRenderer(new NoOpTemplateRenderer())
        .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8) + "\n" + format)
        .build();
  }
}
