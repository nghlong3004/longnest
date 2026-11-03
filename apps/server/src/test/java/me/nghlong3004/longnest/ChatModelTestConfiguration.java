package me.nghlong3004.longnest;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
class ChatModelTestConfiguration {

  @Bean
  ChatClient testChatClient(ChatModel model) {
    return ChatClient.builder(model).build();
  }
}
