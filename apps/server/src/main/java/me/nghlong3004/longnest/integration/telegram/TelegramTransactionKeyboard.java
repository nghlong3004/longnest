package me.nghlong3004.longnest.integration.telegram;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.model.enums.TransactionField;
import me.nghlong3004.longnest.presentation.TransactionActionPresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "longnest.telegram", name = "enabled", havingValue = "true")
public class TelegramTransactionKeyboard {
  private final TransactionActionPresentation presentation;

  InlineKeyboardMarkup empty() {
    return new InlineKeyboardMarkup(List.of());
  }

  InlineKeyboardMarkup card(String token, String nextPage) {
    var rows = new ArrayList<InlineKeyboardRow>();
    if (nextPage != null) rows.add(row("Xem thêm", nextPage));
    rows.add(row("⋯", token + ":m"));
    return new InlineKeyboardMarkup(rows);
  }

  InlineKeyboardMarkup menu(String token) {
    return new InlineKeyboardMarkup(
        List.of(row("Sửa", token + ":e"), row("Xóa", token + ":d"), row("Hủy", token + ":c")));
  }

  InlineKeyboardMarkup fields(String token) {
    var rows = new ArrayList<InlineKeyboardRow>();
    for (var field : TransactionField.values()) {
      rows.add(
          row(
              field == TransactionField.TYPE ? "Loại" : presentation.fieldLabel(field),
              token + ":" + field.getValue()));
    }
    rows.add(row("Hủy", token + ":c"));
    return new InlineKeyboardMarkup(rows);
  }

  InlineKeyboardMarkup cancel(String token) {
    return new InlineKeyboardMarkup(List.of(row("Hủy", token + ":c")));
  }

  InlineKeyboardMarkup save(String token) {
    return new InlineKeyboardMarkup(List.of(row("Lưu", token + ":save"), row("Hủy", token + ":c")));
  }

  InlineKeyboardMarkup delete(String token) {
    return new InlineKeyboardMarkup(
        List.of(row("Xóa vĩnh viễn", token + ":yes"), row("Hủy", token + ":c")));
  }

  private InlineKeyboardRow row(String label, String data) {
    return new InlineKeyboardRow(
        InlineKeyboardButton.builder().text(label).callbackData(data).build());
  }
}
