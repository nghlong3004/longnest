package me.nghlong3004.longnest.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.model.enums.TransactionCategory;
import me.nghlong3004.longnest.model.enums.TransactionField;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TransactionActionPresentationTests {
  private final TransactionActionPresentation presentation =
      new TransactionActionPresentation(new TransactionMessageFormatter());

  @Test
  void rendersCompleteSelectedLegacyTextAndExplicitNullableReplacement() {
    var before = item(TransactionType.EXPENSE, TransactionCategory.FOOD, "🙂".repeat(2500));
    var after = item(TransactionType.EXPENSE, TransactionCategory.FOOD, null);
    assertThat(presentation.previewText(before.id(), before, TransactionField.DESCRIPTION, after))
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
    assertThat(presentation.previewText(before.id(), before, TransactionField.TYPE, after))
        .contains("Trước:\nEXPENSE", "Sau:\nINCOME", "Danh mục (do đổi loại): FOOD → —");
    assertThat(before.category()).isEqualTo(TransactionCategory.FOOD);
  }

  @ParameterizedTest
  @CsvSource({
    "AMOUNT,Số tiền,'45.000,5đ'",
    "DATE,Ngày,2026-11-02",
    "TYPE,Loại thu/chi,EXPENSE",
    "CATEGORY,Danh mục,FOOD",
    "DESCRIPTION,Nội dung,Lunch",
    "LOCATION,Địa điểm,—",
    "TIME_OF_DAY,Buổi,—"
  })
  void rendersTheTypedSelectedField(TransactionField field, String label, String value) {
    var item = item(TransactionType.EXPENSE, TransactionCategory.FOOD, "Lunch");
    assertThat(presentation.fieldLabel(field)).isEqualTo(label);
    assertThat(presentation.instruction(field)).isNotBlank();
    assertThat(presentation.previewText(item.id(), item, field, item))
        .contains("Trường: " + label, "Trước:\n" + value, "Sau:\n" + value);
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
