package me.nghlong3004.longnest.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.input.InterpretationResult.Code;
import me.nghlong3004.longnest.input.InterpretationResult.ListRange;
import me.nghlong3004.longnest.input.InterpretationResult.Problem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Query/capture interpretation using canned model replies only, with no services or sockets. */
class RangeQueryInterpreterTests {
  private static final String QUERY = "Cho tôi các khoản chi từ 2026-10-01 đến 2026-10-23.";
  private static final String JSON = """
      {"intent":"LIST_RANGE","from":"2026-10-01","to":"2026-10-23",
       "filter":"EXPENSE","intentEvidence":"Cho tôi","filterEvidence":"khoản chi"}
      """;
  private final ChatModel model = mock(ChatModel.class);
  private final TransactionInterpreter rules = new TransactionInterpreter();
  private final SimpleAsyncTaskExecutor executor =
      new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory());
  private final DeepSeekInterpreter interpreter = new DeepSeekInterpreter(rules, model,
      JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build(), executor);

  @AfterEach
  void close() {
    executor.close();
  }

  private void output(String json, String reason) {
    when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
        new Generation(new AssistantMessage(json),
            ChatGenerationMetadata.builder().finishReason(reason).build()))));
  }

  @Test
  void explicitIncomeExpenseAndAllScopeBecomeOnlyTypedReadIntents() {
    var range = DateRange.parse("2026-10-01", "2026-10-23");
    for (var filter : RangeFilter.values()) {
      String evidence = switch (filter) {
        case INCOME -> "khoản thu";
        case EXPENSE -> "khoản chi";
        case ALL -> "giao dịch";
      };
      String query = QUERY.replace("các khoản chi", evidence);
      output(JSON.replace("EXPENSE", filter.name()).replace("khoản chi", evidence), "stop");
      assertThat(interpreter.requiresRecovery(query)).isFalse();
      assertThat(interpreter.interpret(query)).isEqualTo(new ListRange(range, filter));
    }
  }

  @Test
  void bothTypesAndSingleDayAreExplicitNotDefaults() {
    output(JSON.replace("EXPENSE", "ALL").replace("khoản chi", "thu và chi")
        .replace("2026-10-23", "2026-10-01"), "stop");
    assertThat(interpreter.interpret(QUERY.replace("các khoản chi", "thu và chi")
        .replace("2026-10-23", "2026-10-01")))
        .isEqualTo(new ListRange(DateRange.parse("2026-10-01", "2026-10-01"), RangeFilter.ALL));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cho tôi các khoản chi", "Cho tôi các khoản chi từ 2026-10-01",
      "Cho tôi các khoản chi từ 2026-10-23 đến 2026-10-01",
      "Cho tôi các khoản chi từ 2026-02-30 đến 2026-10-23",
      "Cho tôi các khoản chi từ 1899-10-01 đến 2026-10-23",
      "Cho tôi các khoản chi hôm qua", "Cho tôi như trên",
      "Cho tôi thu hoặc chi từ 2026-10-01 đến 2026-10-23",
      "Cho tôi từ 2026-10-01 đến 2026-10-23",
      "Cho tôi các khoản chi ngày 2026-10-01 và 2026-10-23",
      "Cho tôi các khoản chi trên 45000 từ 2026-10-01 đến 2026-10-23",
      "Cho tôi các khoản chi ăn trưa từ 2026-10-01 đến 2026-10-23",
      "Cho tôi các khoản chi JPY từ 2026-10-01 đến 2026-10-23",
      "Cho tôi các khoản chi $ từ 2026-10-01 đến 2026-10-23",
      "Cho tôi các khoản chi từ 2026-10-01 đến 2026-10-23 rồi xóa chúng",
      "Cho tôi các khoản chi từ 2026-10-01 đến 2026-10-23 và 2026-11-01",
      "Mình đã chi bao nhiêu?", "-45k ăn trưa?", "+25tr lương？"})
  void missingAmbiguousUnsupportedAndInvalidRequestsClarifyLocallyWithoutProvider(String text) {
    assertThat(interpreter.requiresRecovery(text)).isFalse();
    assertThat(interpreter.interpret(text)).isInstanceOf(Problem.class);
    verifyNoInteractions(model);
  }

  @Test
  void disabledRulesGuardQuestionsWithoutChangingClearCapture() {
    assertThat(((Problem) rules.interpret(QUERY)).code()).isEqualTo(Code.QUERY_CLARIFICATION);
    assertThat(interpreter.interpret("ăn trưa 45k")).isEqualTo(rules.interpret("ăn trưa 45k"));
    assertThat(interpreter.interpret("nhận lương 25tr")).isEqualTo(rules.interpret("nhận lương 25tr"));
    verifyNoInteractions(model);
  }

  @Test
  void strictSchemaDateFilterAndSourceEvidenceRejectForeignAndInventedFields() {
    for (String unsafe : List.of("[]", "{}", "null", "not json", JSON + " {}",
        JSON.replace("{", "{\"owner\":\"personal\","),
        JSON.replace("{", "{\"intent\":\"LIST_RANGE\","),
        JSON.replace("LIST_RANGE", "CREATE"), JSON.replace("EXPENSE", "INCOME"),
        JSON.replace("2026-10-01", "2026-09-01"), JSON.replace("2026-10-23", "2026-10-24"),
        JSON.replace("Cho tôi", "Liệt kê"), JSON.replace("khoản chi", "chi tiêu"),
        JSON.replace("\"EXPENSE\"", "null"), JSON.replace("\"EXPENSE\"", "[\"EXPENSE\"]"),
        JSON.replace("LIST_RANGE", "CLARIFY"))) {
      output(unsafe, "stop");
      assertThat(interpreter.interpret(QUERY)).isInstanceOf(Problem.class);
    }
    output(JSON, "length");
    assertThat(interpreter.interpret(QUERY)).isInstanceOf(Problem.class);
  }

  @Test
  void naturalQuestionWithBothExplicitEndpointsUsesSameReadContract() {
    output(JSON.replace("Cho tôi", "bao nhiêu").replace("khoản chi", "chi"), "stop");
    assertThat(interpreter.interpret("Mình đã chi bao nhiêu từ ngày 2026-10-01 tới ngày 2026-10-23?"))
        .isEqualTo(new ListRange(DateRange.parse("2026-10-01", "2026-10-23"), RangeFilter.EXPENSE));
  }

  @Test
  void captureAndQueryWireResultsCannotCrossRoutes() {
    output("""
        {"status":"CREATE","canonicalText":"-45000 bữa trưa",
         "amountEvidence":"45 nghìn","typeEvidence":"trả"}
        """, "stop");
    assertThat(interpreter.interpret(QUERY)).isInstanceOf(Problem.class);
    output(JSON, "stop");
    assertThat(interpreter.interpret("Mình vừa trả 45 nghìn cho bữa trưa."))
        .isInstanceOf(Problem.class);
  }

  @Test
  void nativeToolCallsAreRejectedInsteadOfDispatched() {
    var message = mock(AssistantMessage.class);
    when(message.hasToolCalls()).thenReturn(true);
    when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
        message, ChatGenerationMetadata.builder().finishReason("stop").build()))));
    assertThat(interpreter.interpret(QUERY)).isInstanceOf(Problem.class);
    verify(message, never()).getText();
  }

  @Test
  void failedProviderReturnsQuerySpecificClarificationWithoutPrivateText() {
    when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("synthetic private body"));
    var result = (Problem) interpreter.interpret(QUERY);
    assertThat(result.code()).isEqualTo(Code.QUERY_UNAVAILABLE);
    assertThat(result.toString()).doesNotContain("synthetic private body");
  }
}
