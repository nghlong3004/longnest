package me.nghlong3004.longnest.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.InterpretationResult;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.model.LedgerException.Code;
import me.nghlong3004.longnest.telegram.mapper.TelegramReplies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

public class UiMessagesTests {
  public static UiMessages vietnamese() {
    var source = new ResourceBundleMessageSource();
    source.setBasename("messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    return new UiMessages(source);
  }

  private final UiMessages messages = vietnamese();
  private final TelegramReplies replies = new TelegramReplies(messages);

  private TransactionView view(String amount, String note) {
    return new TransactionView("9223372036854775807", "1", TransactionType.EXPENSE, amount,
        "VND", Category.FOOD, note, LocalDate.parse("2026-10-20"),
        Instant.parse("2026-10-20T00:00:00Z"), Optional.empty(), TransactionView.State.ACTIVE);
  }

  @Test
  void springAutoConfigurationInjectsIndependentVietnameseAccessorAndRootFallback() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class))
        .withUserConfiguration(UiMessages.class)
        .withPropertyValues("spring.messages.encoding=UTF-8",
            "spring.messages.fallback-to-system-locale=false")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(UiMessages.class).text("button.next")).isEqualTo("Trang sau");
          var source = context.getBean(MessageSource.class);
          assertThat(source.getMessage("ui.unavailable", null, Locale.FRENCH))
              .isEqualTo("Không thể hiển thị thông tin lúc này.");
          assertThat(context.getBean(UiMessages.class).text("missing.future.key", "private"))
              .isEqualTo("Không thể hiển thị thông tin lúc này.").doesNotContain("private");
        });
  }

  @Test
  void hostLocaleCannotChangeFixedVietnameseOutput() {
    Locale original = Locale.getDefault();
    try {
      Locale.setDefault(Locale.US);
      assertThat(messages.text("receipt.saved")).isEqualTo("Đã lưu");
      Locale.setDefault(Locale.JAPAN);
      assertThat(messages.text("button.previous")).isEqualTo("Trang trước");
    } finally {
      Locale.setDefault(original);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "1000000000000"})
  void receiptPreservesExactValuesAndLiteralUserText(String amount) {
    String note = "O'Brien {0} <b>& _*\\ ghi chú";
    var receipt = new MutationReceipt("tg:123:9", "create", true, false, view(amount, note), "2");
    assertThat(replies.receipt(receipt)).startsWith("Đã lưu\n")
        .contains("#9223372036854775807", "Phiên bản 1", "Chi tiêu: " + amount + " VND",
            "2026-10-20 (Asia/Ho_Chi_Minh)", "Danh mục: Ăn uống", "Ghi chú: " + note,
            "Mã thao tác: tg:123:9");
    assertThat(replies.receipt(new MutationReceipt("tg:123:9", "create", true, true,
        receipt.transaction(), "2"))).startsWith("Kết quả đã lưu ban đầu (phát lại)\n");
  }

  @ParameterizedTest
  @EnumSource(Category.class)
  void everyCategoryHasVietnameseLabel(Category category) {
    assertLocalized("entry.category." + category);
  }

  @ParameterizedTest
  @EnumSource(TransactionType.class)
  void everyTypeHasVietnameseLabel(TransactionType type) {
    assertLocalized("entry.type." + type);
  }

  @ParameterizedTest
  @EnumSource(TransactionView.State.class)
  void everyStateHasVietnameseLabel(TransactionView.State state) {
    assertLocalized("entry.state." + state);
  }

  @ParameterizedTest
  @EnumSource(Code.class)
  void everyServiceErrorHasExplanationAndKeepsMachineCode(Code code) {
    assertLocalized("error." + code);
    var failure = new LedgerResult.Failure<>(code, Optional.of("tg:123:9"),
        code == Code.NOT_SAVED || code == Code.OUTCOME_UNKNOWN);
    assertThat(replies.failure(failure)).contains("Mã lỗi: " + code, "Mã thao tác: tg:123:9");
    assertThat(replies.recoveryFailure(failure))
        .contains("Khôi phục đã dừng; cần người vận hành xử lý.").doesNotContain("tự thử lại");
  }

  @ParameterizedTest
  @EnumSource(InterpretationResult.Code.class)
  void everyInterpreterErrorHasLocalizedExplanation(InterpretationResult.Code code) {
    assertLocalized("error." + code);
  }

  @ParameterizedTest
  @EnumSource(value = InterpretationResult.Code.class, names = {"QUERY_CLARIFICATION"})
  void readClarificationsAskNaturallyForDatesWithoutCaptureOrSavedClaims(InterpretationResult.Code code) {
    var problem = new InterpretationResult.Problem(InterpretationResult.Status.NEEDS_CLARIFICATION,
        code, "text", "Cho tôi các khoản chi từ 2026-10-01 đến 2026-10-23.");
    assertThat(replies.problem(problem)).startsWith("Mình")
        .contains("hai ngày cụ thể", "YYYY-MM-DD", problem.example(), "Chưa có dữ liệu nào bị thay đổi")
        .doesNotContain("Đã lưu", "Vui lòng gửi lại một giao dịch", "QUERY_", "amountMinor");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ăn trưa", "45k", "mua sách 80k", "ăn trưa 0", "+45k ăn trưa",
      "ăn trưa 45.000", "ăn trưa 45k; cà phê 20k", "-45k sách 2"})
  void reachableClarificationsRenderVietnameseWithoutLeakingFieldNames(String input) {
    var problem = (InterpretationResult.Problem) new TransactionInterpreter().interpret(input);
    String text = replies.problem(problem);
    assertThat(text).startsWith("Chưa lưu: ").contains("Vui lòng gửi lại", problem.example())
        .doesNotContain("amountMinor", "NEEDS_CLARIFICATION", "UNSUPPORTED", "REJECTED",
            "Không thể hiển thị");
  }

  @Test
  void helpPageAndButtonsUseLocalizedTextButNotTranslatedCommandsOrTokens() {
    assertThat(replies.help()).contains("Gửi một giao dịch", "/recent", "/show ID",
        "/overview", "/income", "/expenses", "/transactions")
        .doesNotContain("chưa được triển khai");
    var page = new TransactionPage("recent", List.of(view("45000", "ăn trưa")), 10,
        "2", Optional.of("opaque-cursor"));
    assertThat(replies.page(page)).contains("tối đa 10", "Chi tiêu", "45000 VND",
        "Trang sau: /recent opaque-cursor");
    var keyboard = replies.keyboard(page, "opaque-token", true).getKeyboard();
    assertThat(keyboard.getFirst().getFirst().getText()).isEqualTo("Xem #9223372036854775807");
    assertThat(keyboard.getLast()).extracting(button -> button.getText())
        .containsExactly("Trang trước", "Trang sau");
    assertThat(keyboard.getLast()).extracting(button -> button.getCallbackData())
        .containsExactly("b:opaque-token:p", "b:opaque-token:n");
    assertThat(replies.page(new TransactionPage("recent", List.of(), 10, "0", Optional.empty())))
        .contains("Chưa có giao dịch.");
  }

  private void assertLocalized(String key) {
    assertThat(messages.text(key)).isNotBlank().doesNotContain("Không thể hiển thị", key);
  }
}
