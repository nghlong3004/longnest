package me.nghlong3004.longnest.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import me.nghlong3004.longnest.input.InterpretationResult.Code;
import me.nghlong3004.longnest.input.InterpretationResult.Problem;
import me.nghlong3004.longnest.input.InterpretationResult.Ready;
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

/** Fake model only; no provider, identity, transaction, database or socket. */
class DeepSeekInterpreterTests {
  private static final String PROSE = "Mình vừa trả 45 nghìn cho bữa trưa.";
  private static final String VALID = """
      {"status":"CREATE","canonicalText":"-45000 bữa trưa",
       "amountEvidence":"45 nghìn","typeEvidence":"trả"}
      """;
  private final ChatModel model = mock(ChatModel.class);
  private final TransactionInterpreter rules = new TransactionInterpreter();
  private final SimpleAsyncTaskExecutor executor =
      new SimpleAsyncTaskExecutor(Thread.ofVirtual().factory());
  private final DeepSeekInterpreter interpreter = new DeepSeekInterpreter(rules, model,
      JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build(), executor);

  private void output(String text, String finish) {
    when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
        new Generation(new AssistantMessage(text),
            ChatGenerationMetadata.builder().finishReason(finish).build()))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ăn trưa 45k", "nhận lương 25tr", "ăn trưa", "45k",
      "+45k ăn trưa", "ăn trưa 0k", "ăn trưa 45.000", "-45k USD đồ dùng",
      "hôm qua ăn trưa 45k", "chuyển tiền 45k", "/delete 42", "ăn trưa 45k; xăng 20k",
      "Mình trả 0k", "Mình trả 45.000", "Mình trả 1e3", "Mình trả 45k USD",
      "Mình trả 45k hoặc 50k", "Mình trả −45k", "Mình trả 45 k", "Mình trả 45k vnd",
      "Mình trả 45 yen", "Mình trả 45k ngày 20", "Mình trả 1000000000001",
      "sách 45k", "Mình nhận lương rồi trả 45k", "Mình chưa trả 45k", "Mình trả khoảng 45k"})
  void deterministicAndUnsafePathsNeverReachProvider(String text) {
    assertThat(interpreter.requiresRecovery(text)).isFalse();
    assertThat(interpreter.interpret(text)).isEqualTo(rules.interpret(text));
    verifyNoInteractions(model);
  }

  @Test
  void evidenceCheckedProseBecomesSameValidatedDraft() {
    output(VALID, "stop");
    assertThat(interpreter.requiresRecovery(PROSE)).isTrue();
    assertThat(interpreter.interpret(PROSE)).isEqualTo(rules.interpret("-45000 bữa trưa"));
  }

  @Test
  void bareAmountIsNeverRescaled() {
    output(VALID.replace("45 nghìn", "45"), "stop");
    assertThat(interpreter.interpret("Mình vừa trả 45 cho bữa trưa."))
        .isInstanceOf(Problem.class);
  }

  @Test
  void incomeUsesSameCanonicalMoneyAndNoInventedCategory() {
    output(VALID.replace("-45000 bữa trưa", "+25000000 tiền thưởng")
        .replace("45 nghìn", "25 triệu").replace("trả", "nhận"), "stop");
    assertThat(interpreter.interpret("Mình vừa nhận 25 triệu tiền thưởng"))
        .isEqualTo(rules.interpret("+25000000 tiền thưởng"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{}", "[]", "null", "not json",
      "{\"status\":\"CLARIFY\",\"canonicalText\":\"\",\"amountEvidence\":\"\",\"typeEvidence\":\"\"}"})
  void malformedAndAmbiguousOutputDoesNotProduceReady(String text) {
    output(text, "stop");
    assertThat(interpreter.interpret(PROSE)).isInstanceOf(Problem.class);
  }

  @Test
  void rejectsUnknownOrDuplicateFieldsTrailingJsonTruncationAndHallucinatedEvidence() {
    for (String unsafe : List.of(VALID.replace("45000", "450000"),
        VALID.replace("trả", "nhận"), VALID.replace("-45000 bữa trưa", "+45000 lương"),
        VALID.replace("bữa trưa", "cà phê"),
        VALID.replace("45 nghìn", "45k"), VALID.replace("\"CREATE\"", "null"),
        VALID.replace("{", "{\"owner\":\"personal\","),
        VALID.replace("{", "{\"status\":\"CREATE\","), VALID + " {}",
        VALID.replace("-45000 bữa trưa", "-45000 ăn trưa; -20k xăng"))) {
      output(unsafe, "stop");
      assertThat(interpreter.interpret(PROSE)).isInstanceOf(Problem.class);
    }
    output(VALID, "length");
    assertThat(interpreter.interpret(PROSE)).isInstanceOf(Problem.class);
  }

  @Test
  void nullOrFailedProviderIsSanitizedAndNeverReady() {
    when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("private synthetic data"));
    var result = (Problem) interpreter.interpret(PROSE);
    assertThat(result.code()).isEqualTo(Code.INTERPRETER_UNAVAILABLE);
    assertThat(result.toString()).doesNotContain("private synthetic data");
    when(model.call(any(Prompt.class))).thenReturn(null);
    assertThat(interpreter.interpret(PROSE)).isInstanceOf(Problem.class);
  }

  @Test
  void conflictingTypeCuesClarifyDespiteSchemaValidModel() {
    output(VALID, "stop");
    assertThat(interpreter.interpret("Mình nhận lương rồi trả 45 nghìn cho bữa trưa."))
        .isInstanceOf(Problem.class);
  }

  @Test
  void overallDeadlineCancelsSlowProviderWithoutRetry() throws Exception {
    var stopped = new CountDownLatch(1);
    when(model.call(any(Prompt.class))).thenAnswer(call -> {
      try {
        new CountDownLatch(1).await();
        return null;
      } finally {
        stopped.countDown();
      }
    });
    assertThat(((Problem) interpreter.interpret(PROSE)).code())
        .isEqualTo(Code.INTERPRETER_UNAVAILABLE);
    assertThat(stopped.await(2, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void interruptionIsPreservedWithoutLeakingProviderFailure() {
    Thread.currentThread().interrupt();
    try {
      assertThat(interpreter.interpret(PROSE)).isInstanceOf(Problem.class);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }
}
