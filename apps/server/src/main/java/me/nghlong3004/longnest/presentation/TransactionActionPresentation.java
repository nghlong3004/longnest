package me.nghlong3004.longnest.presentation;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.model.enums.TransactionField;
import org.springframework.stereotype.Component;

/** Plain-text action presentation only; no session transitions or delivery. */
@Component
@RequiredArgsConstructor
public class TransactionActionPresentation {
  private final TransactionMessageFormatter formatter;

  public static final String EXPIRED =
      "Nút này đã hết hạn hoặc khoản đã thay đổi. Mở lại danh sách giúp em nhé.";
  public static final String CANCELLED =
      "Em đã hủy thao tác rồi nhé. Sổ và bản nháp tạo khoản không thay đổi.";
  public static final String EDIT_CANCELLED =
      "Em đã hủy sửa khoản rồi nhé. Sổ và bản nháp tạo khoản không thay đổi.";
  public static final String CHOOSE_FIELD = "Chọn phần cần sửa giúp em nhé:";
  public static final String DELIVERY_FAILED =
      "Em chưa gửi được phần thao tác. Mở lại ⋯ giúp em nhé.";
  public static final String MUTATION_UNCERTAIN =
      "Em chưa xác nhận được thay đổi. Xem lại sổ trước khi thử lại giúp em nhé.";
  public static final String INPUT_FAILED =
      "Em chưa nhận được tin sửa khoản. Sổ chưa thay đổi; thử lại hoặc bấm Hủy nhé.";
  public static final String SELECT_FIELD_FIRST =
      "Chọn phần cần sửa bằng nút trước rồi gửi giá trị giúp em nhé, hoặc bấm Hủy.";
  public static final String READ_FAILED =
      "Em chưa đọc được khoản này. Sổ chưa thay đổi; thử lại hoặc bấm Hủy nhé.";
  public static final String PREVIEW_PARTS =
      "Em gửi bản xem trước đầy đủ ở các tin bên dưới nhé; xác nhận ở tin cuối hoặc /cancel để"
          + " hủy.";
  public static final String PREVIEW_FAILED =
      "Em chưa gửi đầy đủ bản xem trước. Sổ chưa thay đổi; nhập lại giá trị hoặc bấm Hủy nhé.";
  public static final String STALE_SUMMARY =
      "Em đã cập nhật sổ. Xem danh sách mới bên dưới nhé; danh sách cũ không còn dùng được.";
  public static final String STALE_CARD =
      "Khoản này đã thay đổi. Xem danh sách mới bên dưới giúp em nhé; tin cũ không còn dùng được.";
  public static final String DELETED_CARD =
      "Em đã xóa vĩnh viễn khoản này khỏi sổ. Chưa gỡ được tin cũ này, nên em đã tắt các nút ở đây"
          + " nhé.";

  public String menu(TransactionResponse transaction) {
    return formatter.entry(transaction) + "\nChọn sửa hoặc xóa khoản này để em làm nhé:";
  }

  public String deletionConfirmation(TransactionResponse transaction) {
    return "Xác nhận để em xóa vĩnh viễn khoản này khỏi sổ? Không thể hoàn tác nhé.\nID: "
        + transaction.id()
        + "\n"
        + formatter.entry(transaction);
  }

  public String fieldPrompt(TransactionField field) {
    return instruction(field)
        + "\nGửi em giá trị mới ở tin tiếp theo nhé; /cancel chỉ hủy sửa khoản này.";
  }

  public String invalidInput(TransactionField field) {
    return "Em chưa dùng được giá trị này, hoặc khoản đã thay đổi.\n" + instruction(field);
  }

  public String committed(boolean deleted, boolean refreshed) {
    return (deleted
            ? "Em đã xóa vĩnh viễn khoản này khỏi sổ rồi nhé."
            : "Em đã lưu thay đổi vào sổ rồi nhé.")
        + (refreshed ? "" : " Em chưa cập nhật hết tin hiển thị; xem lại sổ giúp em nhé.");
  }

  public String previewText(
      UUID id, TransactionResponse before, TransactionField field, TransactionResponse after) {
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
              + suppliedValue(before, TransactionField.CATEGORY)
              + " → "
              + suppliedValue(after, TransactionField.CATEGORY);
    }
    return text;
  }

  private String suppliedValue(TransactionResponse item, TransactionField field) {
    var value =
        switch (field) {
          case AMOUNT -> formatter.money(item.amount());
          case DATE -> item.date();
          case TYPE -> item.type();
          case CATEGORY -> item.category();
          case DESCRIPTION -> item.description();
          case LOCATION -> item.location();
          case TIME_OF_DAY -> item.timeOfDay();
        };
    return value == null ? "—" : value.toString();
  }

  public String instruction(TransactionField field) {
    return switch (field) {
      case AMOUNT ->
          "Gửi em số tiền dương, tối đa 2 chữ số lẻ nhé; ví dụ 50000, 50k hoặc 12.50. Không"
              + " dùng dấu phân cách hàng nghìn.";
      case DATE -> "Gửi em ngày theo YYYY-MM-DD nhé, ví dụ 2026-11-02.";
      case TYPE ->
          "Gửi INCOME (thu) hoặc EXPENSE (chi) giúp em nhé. Chuyển sang thu sẽ bỏ danh mục chi"
              + " trong bản xem trước.";
      case CATEGORY ->
          "Chọn giúp em danh mục: FOOD, HOUSING, TRANSPORT, HEALTH, EDUCATION, SHOPPING,"
              + " ENTERTAINMENT, OTHER; - để bỏ.";
      case TIME_OF_DAY ->
          "Chọn giúp em buổi: MORNING (sáng), NOON (trưa), AFTERNOON (chiều), EVENING (tối); -"
              + " để bỏ nhé.";
      case DESCRIPTION, LOCATION -> "Gửi em nội dung mới (tối đa 500 ký tự), hoặc - để bỏ nhé.";
    };
  }

  public String fieldLabel(TransactionField field) {
    return switch (field) {
      case AMOUNT -> "Số tiền";
      case DATE -> "Ngày";
      case TYPE -> "Loại thu/chi";
      case CATEGORY -> "Danh mục";
      case DESCRIPTION -> "Nội dung";
      case LOCATION -> "Địa điểm";
      case TIME_OF_DAY -> "Buổi";
    };
  }
}
