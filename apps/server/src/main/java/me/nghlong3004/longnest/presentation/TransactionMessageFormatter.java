package me.nghlong3004.longnest.presentation;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import me.nghlong3004.longnest.model.dto.TransactionData;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionResponse;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.springframework.stereotype.Component;

/** Vietnamese plain text; financial confirmations retain committed database values. */
@Component
public class TransactionMessageFormatter {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public String confirmation(TransactionData saved) {
    return confirmation(saved, null);
  }

  /** Uses an optional trusted display label already sanitized by the channel message DTO. */
  public String confirmation(TransactionData saved, String displayName) {
    var description = saved.description();
    var name = DisplayNamePresentation.usable(displayName);
    var detail = description == null || description.isBlank() ? "" : " — " + description;
    return "Em đã ghi khoản "
        + type(saved.type())
        + " "
        + money(saved.amount())
        + detail
        + " rồi nhé"
        + (name == null ? "" : ", " + name)
        + ".";
  }

  public String greeting(String displayName) {
    var name = DisplayNamePresentation.usable(displayName);
    return "Em chào"
        + (name == null ? "" : " " + name)
        + ", cần ghi hay xem thu chi cứ nhắn em nhé.";
  }

  public String cancellationConfirmation(TransactionData data) {
    var description = data.description();
    var label =
        description != null && !description.isBlank()
            ? "“" + shorten(description, 120) + "”"
            : data.type() == null ? "đang bổ sung" : type(data.type());
    var amount = data.amount() == null ? "" : " (" + money(data.amount()) + ")";
    return "Em hủy khoản " + label + amount + " rồi nhé.";
  }

  public String summary(TransactionPage page) {
    var filter = page.filter();
    var range = DATE.format(filter.from()) + " → " + DATE.format(filter.to());
    if (page.totals().count() == 0) {
      return "Em chưa thấy khoản " + type(filter.type()) + " nào từ " + range + " nhé.";
    }
    return "Em tìm được các khoản "
        + type(filter.type())
        + " từ "
        + range
        + "\nTổng: "
        + money(page.totals().amount())
        + " · "
        + page.totals().count()
        + " khoản"
        + "\nEm gửi "
        + page.items().size()
        + " khoản trong lượt này.";
  }

  public String entry(TransactionResponse item) {
    var text =
        new StringBuilder(DATE.format(item.date())).append(" · ").append(money(item.amount()));
    if (item.timeOfDay() != null) {
      var time =
          switch (item.timeOfDay()) {
            case MORNING -> "Sáng";
            case NOON -> "Trưa";
            case AFTERNOON -> "Chiều";
            case EVENING -> "Tối";
          };
      text.append(" · ").append(time);
    }
    text.append("\n")
        .append(
            item.description() == null || item.description().isBlank()
                ? "Chưa có nội dung"
                : shorten(item.description(), 240));
    if (item.location() != null && !item.location().isBlank()) {
      text.append("\nĐịa điểm: ").append(shorten(item.location(), 120));
    }
    return text.toString();
  }

  private String type(TransactionType type) {
    return type == TransactionType.INCOME ? "thu" : "chi";
  }

  public String money(BigDecimal amount) {
    var numbers = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
    numbers.setMaximumFractionDigits(2);
    return numbers.format(amount) + "đ";
  }

  private String shorten(String value, int limit) {
    var text = value.strip();
    return text.codePointCount(0, text.length()) > limit
        ? text.substring(0, text.offsetByCodePoints(0, limit)) + "…"
        : text;
  }
}
