package me.nghlong3004.longnest.integration.telegram;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.service.TransactionMessageFormatter;
import org.springframework.stereotype.Component;

/** Plain-text action presentation only; no session transitions or delivery. */
@Component
@RequiredArgsConstructor
public class TelegramTransactionPresentation {
  private static final String EXPIRED =
      "Nút này đã hết hạn hoặc khoản đã thay đổi. Mở lại danh sách giúp em nhé.";
  private final TransactionMessageFormatter formatter;

  public String previewText(
      UUID id, TransactionResponse before, String field, TransactionResponse after) {
    boolean derivedCategoryChange =
        before.type() != after.type() && before.category() != after.category();
    var text =
        "Em gửi bản xem trước để kiểm tra lại nhé.\nID: "
            + id
            + "\n"
            + before.date()
            + " · "
            + before.type()
            + " · "
            + formatter.money(before.amount())
            + "\nTrường: "
            + fieldLabel(field)
            + "\nCác trường khác giữ nguyên"
            + (derivedCategoryChange ? ", trừ danh mục đổi theo loại bên dưới." : ".")
            + "\n\nTrước:\n"
            + suppliedValue(before, field)
            + "\n\nSau:\n"
            + suppliedValue(after, field);
    if (derivedCategoryChange) {
      text +=
          "\nDanh mục (do đổi loại): "
              + suppliedValue(before, "category")
              + " → "
              + suppliedValue(after, "category");
    }
    return text;
  }

  private Object suppliedValue(TransactionResponse item, String field) {
    var value =
        switch (field) {
          case "amount" -> formatter.money(item.amount());
          case "date" -> item.date();
          case "type" -> item.type();
          case "category" -> item.category();
          case "description" -> item.description();
          case "location" -> item.location();
          case "timeOfDay" -> item.timeOfDay();
          default -> throw new IllegalArgumentException(EXPIRED);
        };
    return value == null ? "—" : value;
  }

  public String instruction(String field) {
    return switch (field) {
      case "amount" ->
          "Gửi em số tiền dương, tối đa 2 chữ số lẻ nhé; ví dụ 50000, 50k hoặc 12.50. Không"
              + " dùng dấu phân cách hàng nghìn.";
      case "date" -> "Gửi em ngày theo YYYY-MM-DD nhé, ví dụ 2026-11-02.";
      case "type" ->
          "Gửi INCOME (thu) hoặc EXPENSE (chi) giúp em nhé. Chuyển sang thu sẽ bỏ danh mục chi"
              + " trong bản xem trước.";
      case "category" ->
          "Chọn giúp em danh mục: FOOD, HOUSING, TRANSPORT, HEALTH, EDUCATION, SHOPPING,"
              + " ENTERTAINMENT, OTHER; - để bỏ.";
      case "timeOfDay" ->
          "Chọn giúp em buổi: MORNING (sáng), NOON (trưa), AFTERNOON (chiều), EVENING (tối); -"
              + " để bỏ nhé.";
      case "description", "location" -> "Gửi em nội dung mới (tối đa 500 ký tự), hoặc - để bỏ nhé.";
      default -> throw new IllegalArgumentException(EXPIRED);
    };
  }

  private String fieldLabel(String field) {
    return switch (field) {
      case "amount" -> "Số tiền";
      case "date" -> "Ngày";
      case "type" -> "Loại thu/chi";
      case "category" -> "Danh mục";
      case "description" -> "Nội dung";
      case "location" -> "Địa điểm";
      case "timeOfDay" -> "Buổi";
      default -> throw new IllegalArgumentException(EXPIRED);
    };
  }
}
