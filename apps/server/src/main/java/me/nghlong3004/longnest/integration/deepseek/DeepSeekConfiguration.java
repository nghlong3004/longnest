package me.nghlong3004.longnest.integration.deepseek;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.RestClient;

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
        .options(DeepSeekChatOptions.builder().model(properties.model()).disableThinking().build())
        .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0)))
        .build();
  }

  @Bean
  ChatClient deepSeekChatClient(DeepSeekChatModel model) {
    log.info("DeepSeek chatbot enabled");
    return ChatClient.builder(model)
        .defaultSystem(
            """
            Bạn là trợ lý Longnest. Hãy trò chuyện bằng tiếng Việt, ngắn gọn và tự nhiên.
            Hiện tại bạn chỉ trò chuyện, chưa có khả năng lưu hay sửa dữ liệu.
            Không khẳng định đã ghi nhận giao dịch hoặc cập nhật dữ liệu của người dùng.
            """)
        .build();
  }
}
