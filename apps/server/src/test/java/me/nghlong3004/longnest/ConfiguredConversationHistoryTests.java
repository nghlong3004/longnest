package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import me.nghlong3004.longnest.configuration.LedgerConfiguration;
import me.nghlong3004.longnest.conversation.ConversationHistory;
import me.nghlong3004.longnest.conversation.TransactionModelInterpreter;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.ChatReply;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.presentation.TransactionMessageFormatter;
import me.nghlong3004.longnest.service.DraftService;
import me.nghlong3004.longnest.service.TransactionQueryService;
import me.nghlong3004.longnest.service.TransactionService;
import me.nghlong3004.longnest.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

class ConfiguredConversationHistoryTests {
  @Test
  void rememberingThroughChatServiceUpdatesTheConfiguredSharedHistory() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      new ApplicationContextRunner()
          .withPropertyValues("longnest.ledger.time-zone=Asia/Ho_Chi_Minh")
          .withUserConfiguration(
              LedgerConfiguration.class, ChatServiceImpl.class, TransactionModelInterpreter.class)
          .withBean(Validator.class, factory::getValidator)
          .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
          .withBean(TransactionService.class, () -> mock(TransactionService.class))
          .withBean(DraftService.class, () -> mock(DraftService.class))
          .withBean(TransactionQueryService.class, () -> mock(TransactionQueryService.class))
          .withBean(TransactionMessageFormatter.class)
          .run(
              context -> {
                assertThat(context).hasSingleBean(ConversationHistory.class);
                var first =
                    new ChatMessage(
                        Channel.TELEGRAM, "123", "123", "1", Instant.EPOCH, "Nội dung trước");
                context
                    .getBean(ChatServiceImpl.class)
                    .rememberReply(first, ChatReply.text("Đã hiểu"));
                var next =
                    new ChatMessage(
                        Channel.TELEGRAM,
                        "123",
                        "123",
                        "2",
                        Instant.EPOCH.plusSeconds(1),
                        "Tiếp theo");
                assertThat(context.getBean(ConversationHistory.class).messages(next))
                    .extracting(Message::getText)
                    .containsExactly("Nội dung trước", "Đã hiểu");
              });
    }
  }
}
