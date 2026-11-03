package me.nghlong3004.longnest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;

class TransactionMessageFormatterTests {
  @Test
  void warmConfirmationPreservesCommittedAmountAndLiteralTransactionText() {
    var description = "Anh An trả tiền *Telegram* <b>Thanos</b>";
    var saved =
        new TransactionData(
            TransactionType.EXPENSE,
            new BigDecimal("145000.25"),
            null,
            description,
            null,
            null,
            null);
    assertThat(new TransactionMessageFormatter().confirmation(saved))
        .isEqualTo(
            "Em đã ghi khoản chi 145.000,25đ — Anh An trả tiền *Telegram* <b>Thanos</b> rồi nhé.");
  }
}
