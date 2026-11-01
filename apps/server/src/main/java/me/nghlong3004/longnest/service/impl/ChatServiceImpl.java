package me.nghlong3004.longnest.service.impl;

import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  private final ObjectProvider<ChatClient> clients;

  @Override
  public String reply(String text) {
    var client = clients.getIfAvailable();
    if (client == null) {
      return text;
    }
    String answer;
    try {
      answer = client.prompt().messages(new UserMessage(text)).call().content();
    } catch (RestClientException | TransientAiException | NonTransientAiException exception) {
      throw new ResourceException(ErrorCode.CHATBOT_UNAVAILABLE, exception);
    }
    if (answer == null || answer.isBlank()) {
      throw new ResourceException(ErrorCode.CHATBOT_EMPTY_RESPONSE);
    }
    return answer;
  }
}
