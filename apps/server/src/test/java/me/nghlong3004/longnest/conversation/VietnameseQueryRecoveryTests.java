package me.nghlong3004.longnest.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VietnameseQueryRecoveryTests {
  @ParameterizedTest
  @ValueSource(
      strings = {"Mua vé xem phim 100k", "Chi 100k xem phim", "Xem phim 100k", "Xem thu âm 100k"})
  void expensePurposeIsNotALedgerViewingRequest(String input) {
    assertThat(VietnameseQueryRecovery.interpret(input, null).viewing()).isFalse();
    assertThat(VietnameseQueryRecovery.interpret(input, TransactionType.EXPENSE).viewing())
        .isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Xem chi tiêu gần đây của a đi em",
        "Xem chi hôm nay",
        "Xem chi ngày 01/11/2026",
        "Xem thu tháng trước",
        "Anh muốn xem tất cả khoản chi từ tháng trước tới tháng này",
        "Anh muốn hết tất cả khoản chi từ tháng trước tới tháng này",
        "Xem khoản chi tháng này giúp mình nhé",
        "Xem khoản chi và khoản thu tháng này",
        "Xem khoản chi từ 10/10/2026 đến 20/10/2026",
        "Xem tháng trước"
      })
  void actualLedgerViewingRequestsStayReadOnly(String input) {
    assertThat(VietnameseQueryRecovery.interpret(input, null).viewing()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Thêm 45k", "45k hôm qua", "Ngày 02/11/2026", "Xin chào userId=789"})
  void ambiguousCurrentInputCannotBorrowAFinancialDirectiveFromHistory(String input) {
    assertThat(VietnameseQueryRecovery.canCreate(input)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"Ăn trưa 45k", "Nhận lương 15 triệu", "Chi 145000 cho Anh An", "Mua vé 20k"})
  void explicitNewFinancialInputStillQualifiesForStructuredValidation(String input) {
    assertThat(VietnameseQueryRecovery.canCreate(input)).isTrue();
  }
}
