package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionType;
import me.nghlong3004.longnest.service.TransactionMessageFormatter;
import org.junit.jupiter.api.Test;

class TelegramTransactionPresentationTests {
  private final TelegramTransactionPresentation presentation =
      new TelegramTransactionPresentation(new TransactionMessageFormatter());

  @Test
  void rendersCompleteSelectedLegacyTextAndExplicitNullableReplacement() {
    var before = item(TransactionType.EXPENSE, TransactionCategory.FOOD, "🙂".repeat(2500));
    var after = item(TransactionType.EXPENSE, TransactionCategory.FOOD, null);
    assertThat(presentation.previewText(before.id(), before, "description", after))
        .contains(
            "ID: " + before.id(),
            "45.000,5đ",
            "Các trường khác giữ nguyên.",
            "Trước:\n" + "🙂".repeat(2500),
            "Sau:\n—");
  }

  @Test
  void makesDerivedIncomeCategoryChangeVisibleWithoutChangingTheInputs() {
    var before = item(TransactionType.EXPENSE, TransactionCategory.FOOD, null);
    var after = item(TransactionType.INCOME, null, null);
    assertThat(presentation.previewText(before.id(), before, "type", after))
        .contains("Trước:\nEXPENSE", "Sau:\nINCOME", "Danh mục (do đổi loại): FOOD → —");
    assertThat(before.category()).isEqualTo(TransactionCategory.FOOD);
    assertThatThrownBy(() -> presentation.instruction("owner"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private TransactionResponse item(
      TransactionType type, TransactionCategory category, String description) {
    return new TransactionResponse(
        UUID.fromString("4cf4f284-41c9-4f87-9c30-56203812cfe9"),
        type,
        new BigDecimal("45000.50"),
        category,
        description,
        null,
        LocalDate.of(2026, 11, 2),
        null);
  }
}
