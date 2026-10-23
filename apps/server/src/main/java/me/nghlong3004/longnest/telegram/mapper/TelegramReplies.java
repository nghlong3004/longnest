package me.nghlong3004.longnest.telegram.mapper;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.input.InterpretationResult.Problem;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.ConfirmationRequired;
import me.nghlong3004.longnest.ledger.dto.FieldPrompt;
import me.nghlong3004.longnest.ledger.dto.MutationReceipt;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.dto.RangeSummary;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.telegram.model.TelegramNavigation;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.presentation.UiMessages;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

/** Plain text rendering, not financial calculation or generated DTO property copying. */
@RequiredArgsConstructor
public class TelegramReplies {
  private final UiMessages messages;

  public String receipt(MutationReceipt receipt) {
    return messages.text("receipt.body",
        messages.text(receipt.replayed() ? "receipt.replayed"
            : receipt.changed() ? "receipt.saved" : "receipt.unchanged"),
        view(receipt.transaction()), receipt.operationId());
  }

  public String preview(ConfirmationRequired review) {
    return messages.text("control.preview." + review.kind(), review.actionId(),
        view(review.before()), view(review.after()));
  }

  public String prompt(FieldPrompt prompt) {
    String value = switch (prompt.field()) {
      case AMOUNT -> prompt.current().amountMinor() + " VND";
      case TYPE -> messages.text("entry.type." + prompt.current().type());
      case CATEGORY -> messages.text("entry.category." + prompt.current().category());
      case NOTE -> prompt.current().note().isEmpty() ? messages.text("control.emptyNote")
          : prompt.current().note();
      case DATE -> prompt.current().transactionDate().toString();
    };
    return messages.text("control.prompt." + prompt.field(), prompt.before().id(),
        prompt.before().revision(), value)
        + messages.text("control.prompt.current", view(prompt.current()));
  }

  public String invalidPrompt(LedgerResult.Failure<?> failure) {
    return messages.text("control.invalid", messages.text("error." + failure.code()));
  }

  public String stalePrompt() { return messages.text("control.stale"); }

  public String canceled() { return messages.text("control.canceled"); }

  public String fieldMenu(TransactionView entry) {
    return messages.text("control.field.menu", entry.id(), entry.revision());
  }

  public InlineKeyboardMarkup entryKeyboard(TransactionView entry, String token) {
    if (entry.state() != TransactionView.State.ACTIVE) {
      throw new IllegalArgumentException("Deleted entries require a durable Restore control");
    }
    var rows = new ArrayList<InlineKeyboardRow>();
    rows.add(new InlineKeyboardRow(button(messages.text("button.edit"),
        "v:" + token + ":e:" + entry.id())));
    rows.add(new InlineKeyboardRow(button(messages.text("button.delete"),
        "v:" + token + ":d:" + entry.id())));
    return new InlineKeyboardMarkup(rows);
  }

  public InlineKeyboardMarkup restoreKeyboard(String actionId) {
    return new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(button(
        messages.text("button.restore"), "a:" + actionId + ":r"))));
  }

  public InlineKeyboardMarkup fieldKeyboard(TransactionView entry, String token) {
    var rows = new ArrayList<InlineKeyboardRow>();
    for (EditField field : EditField.values()) {
      rows.add(new InlineKeyboardRow(button(messages.text("button.field." + field),
          "v:" + token + ":f" + field.ordinal() + ":" + entry.id())));
    }
    return new InlineKeyboardMarkup(rows);
  }

  public InlineKeyboardMarkup promptKeyboard(FieldPrompt prompt) {
    var rows = new ArrayList<InlineKeyboardRow>();
    if (prompt.field() == EditField.TYPE) {
      for (TransactionType type : TransactionType.values()) {
        rows.add(new InlineKeyboardRow(button(messages.text("entry.type." + type),
            "p:" + prompt.promptId() + ":v:" + type)));
      }
    }
    if (prompt.field() == EditField.CATEGORY) {
      for (Category category : Category.values()) {
        if (category.supports(prompt.current().type())) {
          rows.add(new InlineKeyboardRow(button(messages.text("entry.category." + category),
              "p:" + prompt.promptId() + ":v:" + category)));
        }
      }
    }
    if (prompt.field() == EditField.NOTE) {
      rows.add(new InlineKeyboardRow(button(messages.text("button.clearNote"),
          "p:" + prompt.promptId() + ":x")));
    }
    rows.add(new InlineKeyboardRow(button(messages.text("button.cancel"),
        "p:" + prompt.promptId() + ":c")));
    return new InlineKeyboardMarkup(rows);
  }

  public InlineKeyboardMarkup previewKeyboard(ConfirmationRequired review) {
    var rows = new ArrayList<InlineKeyboardRow>();
    String prefix = "a:" + review.actionId() + ":";
    if (review.kind() == ConfirmationRequired.Kind.UPDATE) {
      for (EditField field : EditField.values()) {
        rows.add(new InlineKeyboardRow(button(messages.text("button.field." + field),
            prefix + "f" + field.ordinal())));
      }
    }
    rows.add(new InlineKeyboardRow(button(messages.text(review.kind()
        == ConfirmationRequired.Kind.DELETE ? "button.confirmDelete" : "button.save"),
        prefix + "s"), button(messages.text("button.cancel"), prefix + "c")));
    return new InlineKeyboardMarkup(rows);
  }

  public String view(TransactionView view) {
    return messages.text("entry.view", view.id(), view.revision(),
        messages.text("entry.state." + view.state()), messages.text("entry.type." + view.type()),
        view.amountMinor(), view.currency(), view.transactionDate().toString(),
        messages.text("entry.category." + view.category()), view.note());
  }

  public String page(TransactionPage page) {
    var text = new StringBuilder(page.summary().map(value -> summary(value) + "\n")
        .orElseGet(() -> messages.text("page.header", Integer.toString(page.pageSize()))));
    for (var entry : page.entries()) {
      text.append(messages.text("page.row", entry.id(), messages.text("entry.type." + entry.type()),
          entry.amountMinor(), entry.currency(), entry.transactionDate().toString()));
    }
    if (page.entries().isEmpty()) {
      text.append(messages.text("page.empty"));
    }
    if (!page.entries().isEmpty()) text.append(messages.text("page.select"));
    if (page.summary().isEmpty()) {
      page.nextCursor().ifPresent(cursor -> text.append(messages.text("page.next", cursor)));
    }
    return text.toString();
  }

  public String summary(RangeSummary summary) {
    String body;
    if (summary.filter() == RangeFilter.ALL) {
      body = messages.text("range.overview", summary.incomeTotal(), summary.incomeCount(),
          summary.expenseTotal(), summary.expenseCount(), summary.net());
    } else {
      boolean income = summary.filter() == RangeFilter.INCOME;
      body = messages.text("range.type", messages.text("entry.type." + summary.filter()),
          income ? summary.incomeTotal() : summary.expenseTotal(),
          income ? summary.incomeCount() : summary.expenseCount());
    }
    return messages.text("range.header", summary.range().from().toString(),
        summary.range().to().toString(), body);
  }

  public String invalidRange() { return messages.text("range.invalid"); }

  public String problem(Problem problem) {
    return messages.text("problem.body", messages.text("problem.status." + problem.status()),
        messages.text("error." + problem.code()), messages.text("field." + problem.field()),
        problem.example());
  }

  public InlineKeyboardMarkup keyboard(TransactionPage page, String token, boolean previous) {
    var rows = new ArrayList<InlineKeyboardRow>();
    for (var entry : page.entries()) {
      rows.add(new InlineKeyboardRow(
          button(messages.text("button.show", entry.id()),
              TelegramNavigation.pageCallback(page, token, "s:" + entry.id()))));
    }
    var controls = new InlineKeyboardRow();
    if (previous) controls.add(button(messages.text("button.previous"),
        TelegramNavigation.pageCallback(page, token, "p")));
    if (page.nextCursor().isPresent()) {
      controls.add(button(messages.text("button.next"), TelegramNavigation.pageCallback(page, token, "n")));
    }
    if (!controls.isEmpty()) rows.add(controls);
    return rows.isEmpty() ? null : new InlineKeyboardMarkup(rows);
  }

  private InlineKeyboardButton button(String text, String data) {
    return InlineKeyboardButton.builder().text(text).callbackData(data).build();
  }

  public String failure(LedgerResult.Failure<?> failure) {
    return failureText(failure, true);
  }

  public String recoveryFailure(LedgerResult.Failure<?> failure) {
    return failureText(failure, false);
  }

  private String failureText(LedgerResult.Failure<?> failure, boolean automaticRetry) {
    String advice = automaticRetry
        ? (failure.retryable() ? messages.text("failure.retry") : "")
        : messages.text("failure.recovery");
    return messages.text("failure.body", messages.text(failure.code().name().equals("OUTCOME_UNKNOWN")
        ? "failure.unknown" : "failure.unavailable"), messages.text("error." + failure.code()),
        failure.code().name(), failure.operationId()
            .map(id -> messages.text("failure.operation", id)).orElse(""), advice);
  }

  public String help() {
    return messages.text("help.body");
  }
}
