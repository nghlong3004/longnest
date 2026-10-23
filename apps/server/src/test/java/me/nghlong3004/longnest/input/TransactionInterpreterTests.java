package me.nghlong3004.longnest.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.text.Normalizer;
import java.util.Currency;
import java.util.Locale;
import java.util.Random;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.InterpretationResult.Code;
import me.nghlong3004.longnest.input.InterpretationResult.FailureKind;
import me.nghlong3004.longnest.input.InterpretationResult.Problem;
import me.nghlong3004.longnest.input.InterpretationResult.Ready;
import me.nghlong3004.longnest.input.InterpretationResult.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionInterpreterTests {
  private final TransactionInterpreter interpreter = new TransactionInterpreter();

  @ParameterizedTest
  @ValueSource(strings = {"ăn trưa hết 45k", "ăn trưa 45 nghìn", "45 nghìn cho bữa trưa",
      "-45k đồ dùng phòng 2", "ăn trưa 45k; cà phê 20k", "xem thu\nchi tháng này",
      "2026-10-22 ăn trưa 45k", "mình vừa ăn bún", "45k", "xem\tchi tháng này",
      "45nghìn cho bữa trưa", "ăn trưa 45,5 triệu", "45000 vnd cho bữa trưa"})
  void parserCoverageFailuresAreExplicitlyDistinguishedFromFinancialValidation(String text) {
    assertThat(interpreter.interpret(text)).isInstanceOfSatisfying(Problem.class,
        problem -> assertThat(problem.failureKind()).isEqualTo(FailureKind.RECOGNITION));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ăn trưa 0", "ăn trưa 45.000", "ăn trưa 1000000000001",
      "ăn trưa 45k USD", "-45k USD đồ dùng", "+45k ăn trưa", "ăn trưa\u0000",
      "-45k đ đồ dùng", "ăn trưa 1e6", "2026-02-30 ăn trưa 45k",
      "+45k ăn trưa quán 2", "-45k lương công ty 2", "ăn trưa 1000000000000 triệu",
      "ăn trưa 0.001 triệu", "ăn trưa 45,5 triệu USD"})
  void actualFinancialSafetyFailuresStayLocal(String text) {
    assertThat(interpreter.interpret(text)).isInstanceOfSatisfying(Problem.class,
        problem -> assertThat(problem.failureKind()).isEqualTo(FailureKind.VALIDATION));
  }

  @Test
  void amountlessSourceFactsComeOnlyFromAnOtherwiseCompleteKnownPhrase() {
    var facts = interpreter.missingAmount("  ĂN   TRƯA  ").orElseThrow();
    assertThat(facts.type()).isEqualTo(TransactionType.EXPENSE);
    assertThat(facts.category()).isEqualTo(Category.FOOD);
    assertThat(facts.note().value()).isEqualTo("ĂN TRƯA");
    assertThat(interpreter.missingAmount("nhận lương").orElseThrow().type())
        .isEqualTo(TransactionType.INCOME);
    for (String text : new String[] {"", " ", "mua sách", "ăn trưa?", "ăn trưa hôm qua",
        "+ăn trưa", "-lương", "ăn trưa 0", "ăn trưa 45k", "ăn trưa\n", "ăn trưa; lương"}) {
      assertThat(interpreter.missingAmount(text)).as(text).isEmpty();
    }
    assertThat(interpreter.missingAmount(null)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "ăn sáng, FOOD, EXPENSE", "an sang, FOOD, EXPENSE",
    "ăn trưa, FOOD, EXPENSE", "an trua, FOOD, EXPENSE",
    "ăn tối, FOOD, EXPENSE", "an toi, FOOD, EXPENSE",
    "cà phê, FOOD, EXPENSE", "ca phe, FOOD, EXPENSE",
    "đi chợ, GROCERIES, EXPENSE", "di cho, GROCERIES, EXPENSE",
    "xăng, TRANSPORT, EXPENSE", "xang, TRANSPORT, EXPENSE",
    "tiền nhà, RENT, EXPENSE", "tien nha, RENT, EXPENSE",
    "nhận lương, SALARY, INCOME", "nhan luong, SALARY, INCOME",
    "lương, SALARY, INCOME", "luong, SALARY, INCOME",
    "thu nhập, OTHER_INCOME, INCOME", "thu nhap, OTHER_INCOME, INCOME"
  })
  void everyExplicitPhraseAndAliasWorksUnsignedSignedAndCaseInsensitively(
      String phrase, Category category, TransactionType type) {
    for (String wording : new String[] {phrase, phrase.toUpperCase(Locale.ROOT)}) {
      for (String prefix : new String[] {"", type == TransactionType.INCOME ? "+" : "-"}) {
        var draft = ready(prefix.isEmpty() ? wording + " 45k" : prefix + "45k " + wording);
        assertThat(draft.type()).isEqualTo(type);
        assertThat(draft.category()).isEqualTo(category);
        assertThat(draft.amount().amountMinor()).isEqualTo(45_000);
        assertThat(draft.note().value()).isEqualTo(wording);
      }
      String opposite = type == TransactionType.INCOME ? "-" : "+";
      assertProblem(opposite + "45k " + wording, Status.NEEDS_CLARIFICATION, Code.CONFLICTING_TYPE);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "ăn trưa 45k, EXPENSE, FOOD, 45000, ăn trưa",
    "nhận lương 25tr, INCOME, SALARY, 25000000, nhận lương",
    "-45k ăn trưa, EXPENSE, FOOD, 45000, ăn trưa",
    "+25tr lương, INCOME, SALARY, 25000000, lương",
    "'-1,5tr đồ dùng', EXPENSE, UNCATEGORIZED, 1500000, đồ dùng",
    "- 45k đồ dùng, EXPENSE, UNCATEGORIZED, 45000, đồ dùng",
    "-45 đồ dùng, EXPENSE, UNCATEGORIZED, 45, đồ dùng",
    "-00045 đồ dùng, EXPENSE, UNCATEGORIZED, 45, đồ dùng",
    "-1 đồ dùng, EXPENSE, UNCATEGORIZED, 1, đồ dùng",
    "-1000000000000 đồ dùng, EXPENSE, UNCATEGORIZED, 1000000000000, đồ dùng",
    "-0.01k đồ dùng, EXPENSE, UNCATEGORIZED, 10, đồ dùng"
  })
  void canonicalPositiveExamplesProduceOnlyValidatedDrafts(
      String input, TransactionType type, Category category, long amount, String note) {
    var draft = ready(input);
    assertThat(draft.type()).isEqualTo(type);
    assertThat(draft.category()).isEqualTo(category);
    assertThat(draft.amount().amountMinor()).isEqualTo(amount);
    assertThat(draft.note().value()).isEqualTo(note);
  }

  @ParameterizedTest
  @ValueSource(strings = {"45000", "45000đ", "45000 vnd", "45k"})
  void supportedAmountFormsReachTheSameDraftValue(String amount) {
    assertThat(ready("-" + amount + " đồ dùng").amount().amountMinor()).isEqualTo(45_000);
  }

  @ParameterizedTest
  @CsvSource({
    "ăn trưa, NEEDS_CLARIFICATION, MISSING_AMOUNT",
    "45k, NEEDS_CLARIFICATION, MISSING_TYPE",
    "mua sách 80k, UNSUPPORTED, UNKNOWN_PHRASE",
    "+45k ăn trưa, NEEDS_CLARIFICATION, CONFLICTING_TYPE",
    "-25tr lương, NEEDS_CLARIFICATION, CONFLICTING_TYPE",
    "ăn trưa 45.000, NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT",
    "'ăn trưa 45,000', NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT",
    "ăn trưa 1 000 000, NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT",
    "-1 000 000 đồ dùng, NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT",
    "ăn trưa 1.234tr, NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT",
    "-0.045tr đồ dùng, NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT",
    "ăn trưa 0, REJECTED, INVALID_AMOUNT",
    "--45k ăn trưa, REJECTED, INVALID_AMOUNT",
    "ăn trưa 1e6, REJECTED, INVALID_AMOUNT",
    "ăn trưa 45k; cà phê 20k, REJECTED, UNSUPPORTED_INPUT",
    "hoàn tiền 45k, UNSUPPORTED, UNSUPPORTED_INPUT",
    "+45k hoàn tiền, UNSUPPORTED, UNSUPPORTED_INPUT",
    "ăn trưa 45k hôm qua, UNSUPPORTED, UNSUPPORTED_INPUT"
  })
  void canonicalNegativeExamplesNeverProduceDrafts(String input, Status status, Code code) {
    assertProblem(input, status, code);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "-0.00k đồ dùng", "ăn trưa 0.00tr", "-1000000.01tr đồ dùng",
    "ăn trưa 0,00tr", "-1000000,01TR đồ dùng", "-100000000000000.00k đồ dùng"
  })
  void supportedDecimalSyntaxWithInvalidMagnitudeIsRejected(String input) {
    assertProblem(input, Status.REJECTED, Code.INVALID_AMOUNT);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "-0 đồ dùng", "-1000000000001 đồ dùng", "-999999999999999999999999 đồ dùng",
    "-1e6 đồ dùng", "-NaN đồ dùng", "-45+5k đồ dùng", "-1/2 đồ dùng", "-45USD đồ dùng",
    "-٤٥ đồ dùng", "-４５ đồ dùng", "ăn trưa ٤٥", "ăn trưa ４５",
    "-45k 20k", "-45k cà phê 20k", "-45k sách 2", "-45k sách ②", "-45k sách +",
    "ăn trưa 45k cà phê 20k", "ăn trưa 45 k", "-45 k đồ dùng", "-45 tr đồ dùng",
    "-45k đồ dùng 2026-10-21", "ăn trưa 45k 2026-10-21"
  })
  void malformedAmountsAndDescriptionsNeverPartiallyAccept(String input) {
    assertThat(interpreter.interpret(input)).isInstanceOf(Problem.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"−45k ăn trưa", "＋45k ăn trưa", "－45k ăn trưa", "﹢45k ăn trưa",
    "﹣45k ăn trưa", "⁺45k ăn trưa", "₋45k ăn trưa", "➕45k ăn trưa", "-45k đồ−dùng",
    "-45k đồ dùng \ufb29", "\ufb2945k ăn trưa", "-45k đồ\ufb29dùng"})
  void rejectsUnicodeSignLookalikes(String input) {
    assertProblem(input, Status.REJECTED, Code.INVALID_AMOUNT);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "hoàn tiền", "hoan tien", "chuyển khoản", "chuyen khoan", "chuyển tiền", "chuyen tien",
    "vay", "nợ", "no", "trả nợ", "tra no", "hôm qua", "hom qua", "hôm kia", "hom kia",
    "hôm nay", "hom nay", "ngày mai", "ngay mai"
  })
  void everyUnsupportedCueIsRecognizedAtWordBoundaries(String cue) {
    assertProblem("-45k " + cue, Status.UNSUPPORTED, Code.UNSUPPORTED_INPUT);
    assertProblem("+45k sách (" + cue.toUpperCase(Locale.ROOT) + ")",
        Status.UNSUPPORTED, Code.UNSUPPORTED_INPUT);
  }

  @ParameterizedTest
  @ValueSource(strings = {"+45k lương ăn trưa", "-45k ăn trưa lương", "+1 (ăn trưa)",
    "-1 lương!", "+45k thêm tiền nhà", "-45k quà thu nhập"})
  void conflictingTypeCuesCannotBeOverriddenByTheSign(String input) {
    assertProblem(input, Status.NEEDS_CLARIFICATION, Code.CONFLICTING_TYPE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"novel", "vaymuon", "luongson", "xangria", "luong_son", "nồi",
    "hôm quang", "sách USD", "sách EUR", "hẹn bữa khác", "ăn trưa thêm", "ăn trưa!"})
  void wholeWordFalsePositivesDoNotInventTypeCategoryCurrencyOrDates(String description) {
    var draft = ready("-45k " + description);
    assertThat(draft.category()).isEqualTo(Category.UNCATEGORIZED);
    assertThat(draft.note().value()).isEqualTo(description);
  }

  @Test
  void substringExpenseCueDoesNotConflictWithIncome() {
    assertThat(ready("+45k xangria").type()).isEqualTo(TransactionType.INCOME);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-45k USD đồ dùng", "-45k usd đồ dùng", "-45000 usd đồ dùng",
    "-45k vnd đồ dùng", "-45k đ đồ dùng", "-45k € đồ dùng", "-45000 $ đồ dùng",
    "-45000 ₫ đồ dùng", "-45000 vnd USD đồ dùng", "-45000đ vnd đồ dùng"})
  void currencyBoundaryDoesNotBecomeAnOrdinaryNote(String input) {
    assertProblem(input, Status.REJECTED, Code.INVALID_AMOUNT);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-45000 vnd ăn trưa", "-45000 VND ăn trưa", "-45000 đ ăn trưa",
    "-45000đ ăn trưa", "-45000VND ăn trưa"})
  void integerAmountMayConsumeOneSupportedLabel(String input) {
    var draft = ready(input);
    assertThat(draft.amount().amountMinor()).isEqualTo(45_000);
    assertThat(draft.note().value()).isEqualTo("ăn trưa");
    assertThat(draft.category()).isEqualTo(Category.FOOD);
  }

  @Test
  void everyJdkCurrencyCodeIsReservedOnlyAtTheBoundary() {
    for (var currency : Currency.getAvailableCurrencies()) {
      assertProblem("-45k " + currency.getCurrencyCode() + " đồ dùng",
          Status.REJECTED, Code.INVALID_AMOUNT);
    }
    assertThat(ready("-45k sách usd").note().value()).isEqualTo("sách usd");
  }

  @Test
  void normalizesNfcAndWhitespaceButPreservesHumanCaseAndAccents() {
    var draft = ready("  -  45K\u00a0CA\u0300\u2003PHE\u0302  ");
    assertThat(draft.note().value()).isEqualTo("CÀ PHÊ");
    assertThat(draft.category()).isEqualTo(Category.FOOD);
    assertProblem("cà phe 45k", Status.UNSUPPORTED, Code.UNKNOWN_PHRASE);
  }

  @Test
  void inputLimitUsesRawCodePointsNotUtf16OrPostNormalizationLength() {
    assertThat(ready("-1 " + "😀".repeat(253)).note().value()).isEqualTo("😀".repeat(253));
    assertProblem("-1 " + "😀".repeat(254), Status.REJECTED, Code.INVALID_FIELD);
    assertThat(ready("-1 x" + " ".repeat(252)).note().value()).isEqualTo("x");
    assertProblem("-1 x" + " ".repeat(253), Status.REJECTED, Code.INVALID_FIELD);
    assertProblem("-1 " + "e\u0301".repeat(127), Status.REJECTED, Code.INVALID_FIELD);
    assertThat(ready("-1 " + "e\u0301".repeat(126) + "x").note().value())
        .isEqualTo("é".repeat(126) + "x");
    String expanded = "-" + "0".repeat(80) + "1 " + "\u0344".repeat(100);
    assertThat(ready(expanded).note().value())
        .isEqualTo(Normalizer.normalize("\u0344".repeat(100), Normalizer.Form.NFC));
    assertProblem("-1 " + "\u0344".repeat(129), Status.REJECTED, Code.INVALID_FIELD);
  }

  @ParameterizedTest
  @ValueSource(strings = {"\n", "\r", "\r\n", "\t", "\u0000", "\u001b", "\u007f", "\u0085",
    "\u2028", "\u2029", "\ud800"})
  void unsafeTextCannotDisappearDuringWhitespaceNormalization(String unsafe) {
    assertThat(interpreter.interpret("-45k " + unsafe + "ăn trưa")).isInstanceOf(Problem.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/recent", "/show 42", "/delete 42", "-45k đồ dùng;", "-45k", "-"})
  void commandsMultipleEntriesAndIncompleteMessagesDoNotCreateDrafts(String input) {
    assertThat(interpreter.interpret(input)).isInstanceOf(Problem.class);
  }

  @Test
  void nullEmptyAndResultConstructionAreExplicitAndSafe() {
    assertProblem(null, Status.REJECTED, Code.INVALID_FIELD);
    assertProblem("   ", Status.NEEDS_CLARIFICATION, Code.MISSING_AMOUNT);
    assertThatNullPointerException().isThrownBy(() -> new Ready(null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new Problem(Status.READY, Code.MISSING_AMOUNT, "amountMinor", "-1 sách"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new Problem(Status.REJECTED, Code.INVALID_FIELD, "", "-1 sách"));
  }

  @Test
  void boundedSyntheticUnicodeCorpusAlwaysReturnsAnExplicitResult() {
    var random = new Random(20261021L);
    int[] alphabet = {'a', ' ', '1', '0', '+', '-', ';', '\n', '$', 0x344, 0x1f600, 0xd800};
    for (int sample = 0; sample < 2_000; sample++) {
      var text = new StringBuilder();
      int length = random.nextInt(271);
      for (int position = 0; position < length; position++) {
        text.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
      }
      var result = interpreter.interpret(text.toString());
      assertThat(result).isNotNull();
      if (result instanceof Ready(var draft)) {
        assertThat(
                new TransactionDraft(draft.type(), draft.amount(), draft.category(), draft.note()))
            .isEqualTo(draft);
      }
    }
  }

  private TransactionDraft ready(String input) {
    var result = interpreter.interpret(input);
    assertThat(result).isInstanceOf(Ready.class);
    assertThat(result.status()).isEqualTo(Status.READY);
    return ((Ready) result).draft();
  }

  private void assertProblem(String input, Status status, Code code) {
    var result = interpreter.interpret(input);
    assertThat(result).isInstanceOf(Problem.class);
    var problem = (Problem) result;
    assertThat(problem.status()).isEqualTo(status);
    assertThat(problem.code()).isEqualTo(code);
    assertThat(problem.field()).isNotBlank();
    assertThat(problem.example()).isEqualTo("-45k đồ dùng");
  }
}
