package me.nghlong3004.longnest.input;

import static java.util.Map.entry;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.AMBIGUOUS_AMOUNT;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.CONFLICTING_TYPE;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.INVALID_AMOUNT;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.INVALID_FIELD;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.MISSING_AMOUNT;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.MISSING_TYPE;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.UNKNOWN_PHRASE;
import static me.nghlong3004.longnest.input.InterpretationResult.Code.UNSUPPORTED_INPUT;
import static me.nghlong3004.longnest.input.InterpretationResult.Status.NEEDS_CLARIFICATION;
import static me.nghlong3004.longnest.input.InterpretationResult.Status.REJECTED;
import static me.nghlong3004.longnest.input.InterpretationResult.Status.UNSUPPORTED;

import java.text.Normalizer;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import me.nghlong3004.longnest.domain.Category;
import me.nghlong3004.longnest.domain.Money;
import me.nghlong3004.longnest.domain.Note;
import me.nghlong3004.longnest.domain.TransactionDraft;
import me.nghlong3004.longnest.domain.TransactionType;
import me.nghlong3004.longnest.input.InterpretationResult.Code;
import me.nghlong3004.longnest.input.InterpretationResult.Problem;
import me.nghlong3004.longnest.input.InterpretationResult.Ready;
import me.nghlong3004.longnest.input.InterpretationResult.Status;

/** Stateless finite-grammar interpreter. Host date, identity and persistence remain outside it. */
public final class TransactionInterpreter implements InputInterpreter {
  private static final String EXAMPLE = "-45k đồ dùng";
  private static final Map<String, Category> PHRASES =
      Map.ofEntries(
          entry("ăn sáng", Category.FOOD), entry("an sang", Category.FOOD),
          entry("ăn trưa", Category.FOOD), entry("an trua", Category.FOOD),
          entry("ăn tối", Category.FOOD), entry("an toi", Category.FOOD),
          entry("cà phê", Category.FOOD), entry("ca phe", Category.FOOD),
          entry("đi chợ", Category.GROCERIES), entry("di cho", Category.GROCERIES),
          entry("xăng", Category.TRANSPORT), entry("xang", Category.TRANSPORT),
          entry("tiền nhà", Category.RENT), entry("tien nha", Category.RENT),
          entry("nhận lương", Category.SALARY), entry("nhan luong", Category.SALARY),
          entry("lương", Category.SALARY), entry("luong", Category.SALARY),
          entry("thu nhập", Category.OTHER_INCOME), entry("thu nhap", Category.OTHER_INCOME));
  private static final Pattern KNOWN_CUES = cues(PHRASES.keySet());
  private static final Pattern UNSUPPORTED_CUES =
      cues(Set.of(
          "hoàn tiền", "hoan tien", "chuyển khoản", "chuyen khoan", "chuyển tiền",
          "chuyen tien", "vay", "nợ", "no", "trả nợ", "tra no", "hôm qua", "hom qua",
          "hôm kia", "hom kia", "hôm nay", "hom nay", "ngày mai", "ngay mai"));
  private static final Pattern WHITESPACE = Pattern.compile("[\\s\\p{Z}]+");
  private static final Pattern GROUPED_AMOUNT = Pattern.compile("^[0-9]+(?: [0-9]{3})+(?: |$)");
  // Supported decimal syntax with an invalid magnitude is not a syntax ambiguity.
  private static final Pattern AMBIGUOUS_DECIMAL =
      Pattern.compile("[0-9]+[.,](?:[0-9]+|[0-9]{3,}(?:k|tr))", Pattern.CASE_INSENSITIVE);
  private static final Set<String> CURRENCY_CODES =
      Currency.getAvailableCurrencies().stream()
          .map(Currency::getCurrencyCode)
          .collect(Collectors.toUnmodifiableSet());

  public InterpretationResult interpret(String message) {
    if (message == null) {
      return problem(REJECTED, INVALID_FIELD, "text");
    }
    if (message.indexOf(';') >= 0
        || message.codePoints().anyMatch(TransactionInterpreter::isLineBreak)) {
      return problem(REJECTED, UNSUPPORTED_INPUT, "text");
    }
    if (message.codePointCount(0, message.length()) > Note.MAX_CODE_POINTS
        || message.codePoints()
            .anyMatch(
                cp -> Character.isISOControl(cp) || Character.getType(cp) == Character.SURROGATE)) {
      return problem(REJECTED, INVALID_FIELD, "text");
    }
    String text =
        WHITESPACE.matcher(Normalizer.normalize(message, Normalizer.Form.NFC))
            .replaceAll(" ")
            .trim();
    if (text.isEmpty()) {
      return problem(NEEDS_CLARIFICATION, MISSING_AMOUNT, "amountMinor");
    }
    String lower = text.toLowerCase(Locale.ROOT);
    if (RangeQueryInterpreter.isQuery(text)) {
      return RangeQueryInterpreter.clarify();
    }
    if (text.startsWith("/") || UNSUPPORTED_CUES.matcher(lower).find()) {
      return problem(UNSUPPORTED, UNSUPPORTED_INPUT, "text");
    }
    if (text.codePoints().anyMatch(TransactionInterpreter::isLookalikeSign)) {
      return problem(REJECTED, INVALID_AMOUNT, "amountMinor");
    }
    if (text.startsWith("+") || text.startsWith("-")) {
      return signed(text);
    }
    var category = PHRASES.get(lower);
    if (category != null) {
      return problem(NEEDS_CLARIFICATION, MISSING_AMOUNT, "amountMinor");
    }
    for (var phrase : PHRASES.entrySet()) {
      String prefix = phrase.getKey() + " ";
      if (lower.startsWith(prefix)) {
        return draft(
            text.substring(prefix.length()), typeOf(phrase.getValue()), phrase.getValue(),
            text.substring(0, prefix.length() - 1));
      }
    }
    if (Character.isDigit(text.codePointAt(0))) {
      try {
        Money.parse(text);
        return problem(NEEDS_CLARIFICATION, MISSING_TYPE, "type");
      } catch (IllegalArgumentException exception) {
        return invalidAmount(text);
      }
    }
    return problem(UNSUPPORTED, UNKNOWN_PHRASE, "type");
  }

  private InterpretationResult signed(String text) {
    var type = text.charAt(0) == '+' ? TransactionType.INCOME : TransactionType.EXPENSE;
    String body = text.substring(1).trim();
    if (body.isEmpty() || PHRASES.containsKey(body.toLowerCase(Locale.ROOT))) {
      return problem(NEEDS_CLARIFICATION, MISSING_AMOUNT, "amountMinor");
    }
    if (GROUPED_AMOUNT.matcher(body).find()) {
      return problem(NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT, "amountMinor");
    }
    int space = body.indexOf(' ');
    String amount = space < 0 ? body : body.substring(0, space);
    String description = space < 0 ? "" : body.substring(space + 1);
    String marker = firstToken(description);
    if (marker.equalsIgnoreCase("đ") || marker.equalsIgnoreCase("vnd")) {
      if (!amount.matches("[0-9]+")) {
        return problem(REJECTED, INVALID_AMOUNT, "amountMinor");
      }
      amount += " " + marker;
      description = description.substring(marker.length()).trim();
      marker = firstToken(description);
    }
    // A currency token at this boundary must never silently become a VND note.
    if (isCurrencyMarker(marker)) {
      return problem(REJECTED, INVALID_AMOUNT, "amountMinor");
    }
    if (marker.equalsIgnoreCase("k") || marker.equalsIgnoreCase("tr")) {
      return problem(REJECTED, UNSUPPORTED_INPUT, "amountMinor");
    }
    try {
      Money.parse(amount);
    } catch (IllegalArgumentException exception) {
      return invalidAmount(amount);
    }
    if (description.isEmpty()) {
      return problem(NEEDS_CLARIFICATION, UNSUPPORTED_INPUT, "note");
    }
    if (description.codePoints().anyMatch(TransactionInterpreter::isDescriptionNumberOrSign)) {
      return problem(REJECTED, UNSUPPORTED_INPUT, "note");
    }
    String lower = description.toLowerCase(Locale.ROOT);
    var cues = KNOWN_CUES.matcher(lower);
    while (cues.find()) {
      if (!PHRASES.get(cues.group()).supports(type)) {
        return problem(NEEDS_CLARIFICATION, CONFLICTING_TYPE, "type");
      }
    }
    return draft(amount, type, PHRASES.getOrDefault(lower, Category.UNCATEGORIZED), description);
  }

  private InterpretationResult draft(
      String amount, TransactionType type, Category category, String description) {
    Money money;
    try {
      money = Money.parse(amount);
    } catch (IllegalArgumentException exception) {
      return invalidAmount(amount);
    }
    Note note;
    try {
      note = new Note(description);
    } catch (IllegalArgumentException exception) {
      return problem(REJECTED, INVALID_FIELD, "note");
    }
    return new Ready(new TransactionDraft(type, money, category, note));
  }

  private static Problem invalidAmount(String amount) {
    if (GROUPED_AMOUNT.matcher(amount).find() || AMBIGUOUS_DECIMAL.matcher(amount).matches()) {
      return problem(NEEDS_CLARIFICATION, AMBIGUOUS_AMOUNT, "amountMinor");
    }
    return problem(REJECTED, INVALID_AMOUNT, "amountMinor");
  }

  private static Problem problem(Status status, Code code, String field) {
    return new Problem(status, code, field, EXAMPLE);
  }

  private static TransactionType typeOf(Category category) {
    return category.supports(TransactionType.EXPENSE)
        ? TransactionType.EXPENSE : TransactionType.INCOME;
  }

  private static Pattern cues(Set<String> phrases) {
    String alternatives = phrases.stream().map(Pattern::quote).collect(Collectors.joining("|"));
    return Pattern.compile(
        "(?<![\\p{L}\\p{M}\\p{N}_])(?:" + alternatives + ")(?![\\p{L}\\p{M}\\p{N}_])");
  }

  private static String firstToken(String text) {
    int space = text.indexOf(' ');
    return space < 0 ? text : text.substring(0, space);
  }

  private static boolean isCurrencyMarker(String token) {
    return token.equalsIgnoreCase("đ")
        || CURRENCY_CODES.contains(token.toUpperCase(Locale.ROOT))
        || token.codePoints().anyMatch(cp -> Character.getType(cp) == Character.CURRENCY_SYMBOL);
  }

  private static boolean isLineBreak(int cp) {
    return cp == '\n' || cp == '\r' || cp == 0x85 || cp == 0x2028 || cp == 0x2029;
  }

  private static boolean isLookalikeSign(int cp) {
    return (Character.getType(cp) == Character.DASH_PUNCTUATION && cp != '-')
        || cp == 0x2212 || cp == 0xff0b || cp == 0xfe62 || cp == 0x207a || cp == 0x207b
        || cp == 0x208a || cp == 0x208b || cp == 0x2795 || cp == 0x2796 || cp == 0xfb29;
  }

  private static boolean isDescriptionNumberOrSign(int cp) {
    int type = Character.getType(cp);
    return cp == '+' || cp == '-' || isLookalikeSign(cp)
        || type == Character.DECIMAL_DIGIT_NUMBER || type == Character.LETTER_NUMBER
        || type == Character.OTHER_NUMBER;
  }
}
