package me.nghlong3004.longnest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ObjectProvider<ChatClient> clients;

  public String reply(String text) {
    var client = clients.getIfAvailable();
    if (client == null) {
      return text;
    }
    var answer = client.prompt().messages(new UserMessage(text)).call().content();
    if (answer == null || answer.isBlank()) {
      throw new IllegalStateException("Chatbot returned an empty answer");
    }
    return answer;
  }
}
