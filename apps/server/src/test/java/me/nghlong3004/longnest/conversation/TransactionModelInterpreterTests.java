package me.nghlong3004.longnest.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import me.nghlong3004.longnest.exception.ResourceException;
import me.nghlong3004.longnest.model.dto.ChatMessage;
import me.nghlong3004.longnest.model.dto.DraftContext;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.enums.Channel;
import me.nghlong3004.longnest.model.enums.ChatAction;
import me.nghlong3004.longnest.model.enums.DraftStatus;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.databind.json.JsonMapper;

class TransactionModelInterpreterTests {
  @Test
  void assemblesHistoryNamePendingDraftAndFinalInputInOrderWithTrustedLocalDates() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var history = new ConversationHistory();
      var old = message("1", "Trao đổi trước", Instant.parse("2026-10-31T17:30:00Z"), null);
      history.remember(old, "Trả lời trước", null);
      var model =
          model(
              "{\"action\":\"READY\",\"transaction\":{\"amount\":45000},\"reply\":\"Đủ thông"
                  + " tin\"}");
      var interpreter =
          new TransactionModelInterpreter(
              JsonMapper.builder().build(),
              factory.getValidator(),
              ZoneId.of("Asia/Ho_Chi_Minh"),
              history);
      var draft =
          new DraftContext(
              UUID.randomUUID(),
              DraftStatus.PENDING,
              new TransactionData(
                  TransactionType.EXPENSE,
                  null,
                  null,
                  "Cà phê",
                  null,
                  LocalDate.of(2026, 11, 1),
                  null),
              "Cà phê hôm nay",
              "Bao nhiêu?",
              "3",
              old.sentAt(),
              "2",
              1,
              false);
      var result =
          interpreter.interpret(
              ChatClient.create(model),
              message("4", "45k", Instant.parse("2026-11-01T17:30:00Z"), "Lan"),
              draft);
      assertThat(result.action()).isEqualTo(ChatAction.READY);
      assertThat(result.transaction().type()).isNull();
      assertThat(result.transaction().amount()).isEqualByComparingTo(new BigDecimal("45000"));
      var prompt = ArgumentCaptor.forClass(Prompt.class);
      verify(model).call(prompt.capture());
      var texts = prompt.getValue().getInstructions().stream().map(m -> m.getText()).toList();
      assertThat(texts).hasSize(7);
      assertThat(texts.get(0))
          .contains("Original message date: 2026-11-01", "New message date: 2026-11-02");
      assertThat(texts.subList(1, 3)).containsExactly("Trao đổi trước", "Trả lời trước");
      assertThat(texts.get(3)).contains("DISPLAY_METADATA_JSON", "\"displayName\":\"Lan\"");
      assertThat(texts.get(4)).isEqualTo("Cà phê hôm nay");
      assertThat(texts.get(5))
          .contains(
              "Known draft fields:", "\"date\":\"2026-11-01\"", "Current question: Bao nhiêu?");
      assertThat(texts.get(6)).isEqualTo("45k");
    }
  }

  @Test
  void extractedParserRejectsDuplicateFieldsRatherThanAcceptingLastValue() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var interpreter =
          new TransactionModelInterpreter(
              JsonMapper.builder().build(),
              factory.getValidator(),
              ZoneId.of("Asia/Ho_Chi_Minh"),
              new ConversationHistory());
      var model = model("{\"action\":\"READY\",\"action\":\"CHAT\",\"reply\":\"Xin chào\"}");
      assertThatThrownBy(
              () ->
                  interpreter.interpret(
                      ChatClient.create(model), message("1", "Tin mới", Instant.EPOCH, null), null))
          .isInstanceOfSatisfying(
              ResourceException.class,
              e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_INVALID_RESPONSE));
    }
  }

  private ChatModel model(String answer) {
    var model = mock(ChatModel.class);
    when(model.getOptions()).thenReturn(ChatOptions.builder().build());
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(answer)))));
    return model;
  }

  private ChatMessage message(String id, String text, Instant time, String name) {
    return new ChatMessage(Channel.TELEGRAM, "123", "123", id, time, text, null, name);
  }
}
