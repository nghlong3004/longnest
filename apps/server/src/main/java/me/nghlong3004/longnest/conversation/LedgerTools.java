package me.nghlong3004.longnest.conversation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Locale;
import java.util.Currency;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.DateRange;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.RangeFilter;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.TransactionInterpreter;
import me.nghlong3004.longnest.ledger.adapter.LedgerClient;
import me.nghlong3004.longnest.ledger.dto.LedgerResult;
import me.nghlong3004.longnest.ledger.dto.TransactionPage;
import me.nghlong3004.longnest.ledger.dto.TransactionView;
import me.nghlong3004.longnest.ledger.mapper.TransactionMapper;
import me.nghlong3004.longnest.ledger.model.EditField;
import me.nghlong3004.longnest.ledger.model.HostContext;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Native callbacks over shared services; no confirmation/proof/owner argument is exposed. */
@RequiredArgsConstructor
public final class LedgerTools {
  static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final LedgerClient ledger;
  private final TransactionMapper mapper;

  public enum Period { TODAY, YESTERDAY, THIS_MONTH, LAST_MONTH, EXPLICIT, KEEP }
  public enum Scope { ALL, INCOME, EXPENSE, FOOD, KEEP }
  public enum Missing { AMOUNT, TYPE, DATES, TARGET, AMBIGUOUS, UNSUPPORTED }

  static final class Turn {
    final HostContext host;
    final String text;
    final ConversationMemory.State state;
    ConversationAssistant.Reply reply;

    Turn(HostContext host, String text, ConversationMemory.State state) {
      this.host = host;
      this.text = text;
      this.state = state;
    }

    void result(LedgerResult<?> result) { reply = new ConversationAssistant.Reply(result, null); }
    void clarify(Missing missing) { reply = ConversationAssistant.message("conversation." + missing); }
  }

  @Tool(name = "list_transactions", returnDirect = true, description = "Read transactions and exact full-range totals. Interpret natural Vietnamese dates; use KEEP only for unchanged dimensions of a previous validated query. THIS_MONTH is month-to-date. FOOD is only Category.FOOD, not groceries or note search. Unsupported filters must clarify, never silently discard them.")
  public String list(Period period, Scope scope,
      @ToolParam(required = false, description = "Only for EXPLICIT: start calendar date") String from,
      @ToolParam(required = false, description = "Only for EXPLICIT: inclusive end date") String to,
      ToolContext context) {
    Turn turn = turn(context);
    DateRange range = range(turn, period, from, to);
    RangeFilter filter = scope == Scope.KEEP ? previous(turn).filter()
        : RangeFilter.valueOf(scope.name());
    var result = ledger.list(turn.host, range, filter, Optional.empty());
    if (result instanceof LedgerResult.Ok<TransactionPage>) {
      turn.state.query = new ConversationMemory.Query(range, filter);
    }
    turn.result(result);
    return "handled";
  }

  @Tool(name = "recent_transactions", returnDirect = true, description = "Read the latest ten saved entries, with selectable navigation. No date or category filter.")
  public String recent(ToolContext context) {
    Turn turn = turn(context);
    turn.result(ledger.recent(turn.host, Optional.empty()));
    return "handled";
  }

  @Tool(name = "get_transaction", returnDirect = true, description = "Show current owned entry. ID must be explicitly supplied by the user, or empty to use the host-selected entry. Never guess an ID.")
  public String get(String id, ToolContext context) {
    Turn turn = turn(context);
    String target = target(turn, id).id();
    var result = ledger.get(turn.host, target);
    if (result instanceof LedgerResult.Ok<TransactionView> ok) {
      turn.state.target = new ConversationMemory.Target(ok.data().id(), ok.data().revision());
    }
    turn.result(result);
    return "handled";
  }

  @Tool(name = "create_transaction", returnDirect = true, description = "Immediately save ONE clearly asserted actual income/expense, never a question, hypothetical, transfer/refund/debt or ambiguous amount. amount is the user's literal amount expression, not model arithmetic; currency VND only. Missing type/amount must clarify. Category may be UNCATEGORIZED; note uses user wording. No owner/ID/proof. No date supplied means TODAY.")
  public String create(TransactionType type, String amount, String currency, Category category,
      String note, Period period,
      @ToolParam(required = false, description = "Only for EXPLICIT: financial date") String date,
      ToolContext context) {
    Turn turn = turn(context);
    if (!Money.CURRENCY.equals(currency) || TransactionInterpreter.isQuery(turn.text)
        || TransactionInterpreter.isUnsupportedFinance(turn.text)) {
      throw new IllegalArgumentException("Unsupported capture");
    }
    Money money = amount(turn, amount);
    LocalDate resolved = range(turn, period, date, date).from();
    if (period != Period.TODAY && period != Period.YESTERDAY && period != Period.EXPLICIT) {
      throw new IllegalArgumentException("Capture needs one date");
    }
    if (!note.isEmpty() && !turn.text.contains(note)) throw new IllegalArgumentException("Note source");
    var draft = new TransactionDraft(type, money, category, new Note(note));
    turn.result(ledger.create(turn.host, mapper.input(draft, resolved)));
    return "handled";
  }

  @Tool(name = "preview_update", returnDirect = true, description = "Prepare an immutable edit preview, NEVER Save. Use the host-selected entry or an explicit user ID; change one requested field. AMOUNT uses literal user amount, DATE uses grounded period. Only the actual user's displayed Save/Cancel controls can complete it.")
  public String preview(String id, EditField field, String value, Period period, ToolContext context) {
    Turn turn = turn(context);
    var target = revision(turn, id);
    if (target == null) return "handled";
    String replacement = switch (field) {
      case AMOUNT -> amount(turn, value).canonicalAmount();
      case DATE -> range(turn, period, value, value).from().toString();
      default -> value;
    };
    if (field == EditField.DATE && period != Period.TODAY && period != Period.YESTERDAY
        && period != Period.EXPLICIT) throw new IllegalArgumentException("One date required");
    if (target.revision() == null) {
      turn.clarify(Missing.TARGET);
      return "handled";
    }
    turn.result(ledger.previewUpdate(turn.host, target.id(), target.revision(),
        mapper.fieldPatch(field, replacement), null));
    return "handled";
  }

  @Tool(name = "request_delete_transaction", returnDirect = true, description = "Request a target-bound Delete confirmation, NEVER delete or confirm. Use empty ID for selected entry or an explicit user ID. Actual Confirm Delete/Cancel remains host/user-owned.")
  public String delete(String id, ToolContext context) {
    Turn turn = turn(context);
    turn.result(ledger.requestDeleteCurrent(turn.host, target(turn, id).id()));
    return "handled";
  }

  @Tool(name = "restore_transaction", returnDirect = true, description = "Restore the host-selected deleted entry only on an explicit current user restore request. No second Save gate. Never guess target/revision.")
  public String restore(String id, ToolContext context) {
    Turn turn = turn(context);
    var target = revision(turn, id);
    if (target == null) return "handled";
    turn.result(ledger.restore(turn.host, target.id(), target.revision()));
    return "handled";
  }

  @Tool(name = "clarify", returnDirect = true, description = "Ask a natural Vietnamese clarification for missing/ambiguous/unsupported facts. Never guess missing financial facts, target or unsupported filters. Use DATES only when no temporal reference or usable previous query exists.")
  public String clarify(Missing missing, ToolContext context) {
    turn(context).clarify(missing);
    return "handled";
  }

  private static Turn turn(ToolContext context) {
    if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Turn interrupted");
    Object value = context.getContext().get("turn");
    if (!(value instanceof Turn turn) || turn.reply != null) {
      throw new IllegalStateException("Host turn required");
    }
    return turn;
  }

  private static ConversationMemory.Query previous(Turn turn) {
    if (turn.state.query == null) throw new IllegalArgumentException("No previous query");
    return turn.state.query;
  }

  static DateRange range(Turn turn, Period period, String from, String to) {
    LocalDate today = turn.host.businessReference().atZone(ZONE).toLocalDate();
    return switch (period) {
      case TODAY -> new DateRange(today, today);
      case YESTERDAY -> new DateRange(today.minusDays(1), today.minusDays(1));
      case THIS_MONTH -> new DateRange(today.withDayOfMonth(1), today);
      case LAST_MONTH -> new DateRange(today.minusMonths(1).withDayOfMonth(1),
          today.withDayOfMonth(1).minusDays(1));
      case EXPLICIT -> DateRange.parse(from, to);
      case KEEP -> previous(turn).range();
    };
  }

  private static ConversationMemory.Target target(Turn turn, String id) {
    if (id.isEmpty()) {
      if (turn.state.target == null) throw new IllegalArgumentException("Select target first");
      return turn.state.target;
    }
    if (!id.matches("[1-9][0-9]{0,18}")
        || !Pattern.compile("(?<![\\p{L}\\p{N}_.,+\\-])" + id
            + "(?![\\p{L}\\p{N}_.,\\-])").matcher(turn.text).find()) {
      throw new IllegalArgumentException("Explicit target required");
    }
    return turn.state.target != null && turn.state.target.id().equals(id)
        ? turn.state.target : new ConversationMemory.Target(id, null);
  }

  private ConversationMemory.Target revision(Turn turn, String id) {
    var selected = target(turn, id);
    if (selected.revision() != null) return selected;
    var current = ledger.get(turn.host, selected.id());
    if (current instanceof LedgerResult.Ok<TransactionView> ok) {
      return new ConversationMemory.Target(ok.data().id(), ok.data().revision());
    }
    turn.result(current);
    return null;
  }

  private static Money amount(Turn turn, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Literal current amount required");
    }
    var literal = Pattern.compile("(?<![\\p{L}\\p{N}.,+\\-])" + Pattern.quote(value)
        + "(?![\\p{L}\\p{N}.,])").matcher(turn.text);
    if (!literal.find()) throw new IllegalArgumentException("Whole literal amount required");
    String remainder = turn.text.substring(literal.end()).stripLeading();
    if (remainder.matches("(?iu)^(?:nghìn|ngàn|triệu|k|tr)(?:\\b.*)?$")
        || remainder.matches("^[0-9].*")) throw new IllegalArgumentException("Incomplete amount");
    var currency = Pattern.compile("^(\\p{L}+|\\p{Sc})(?:\\b|$)").matcher(remainder);
    if (currency.find() && (currency.group(1).codePoints()
        .anyMatch(cp -> Character.getType(cp) == Character.CURRENCY_SYMBOL)
        || Currency.getAvailableCurrencies().stream().anyMatch(c -> !c.getCurrencyCode().equals("VND")
            && c.getCurrencyCode().equalsIgnoreCase(currency.group(1))))) {
      throw new IllegalArgumentException("Unsupported currency");
    }
    // Value conversion only, not a phrase parser: exact Vietnamese magnitude labels.
    String normalized = value.toLowerCase(Locale.ROOT).replaceFirst(" *(nghìn|ngàn)$", "k")
        .replaceFirst(" *triệu$", "tr");
    return Money.parse(normalized);
  }
}
