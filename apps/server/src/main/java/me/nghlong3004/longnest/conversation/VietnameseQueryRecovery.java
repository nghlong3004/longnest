package me.nghlong3004.longnest.conversation;

import java.util.Locale;
import java.util.regex.Pattern;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.model.enums.TransactionType;

/** Narrow read-only recovery for the demonstrated query loop, not a general language parser. */
public final class VietnameseQueryRecovery {
  private static final String POLITE_ENDING =
      "(?: (?:giúp|cho) (?:mình|tôi|anh|em|bạn))?(?: nhé| nha| với| đi em)*[.!?]*";
  private static final Pattern LEDGER_VIEW_REQUEST =
      Pattern.compile(
          "(?:(?:anh|mình|tôi) muốn )?xem (?:tất cả |các )?"
              + "(?:(?:khoản (?:chi|thu)|chi tiêu|thu nhập|giao dịch|sổ)(?=$|\\s|[.!?,]).*"
              + "|(?:chi|thu)(?=$|[.!?]| (?:hôm|tháng|từ|ngày|gần|và)(?= |$)).*)");
  private static final Pattern RANGE =
      Pattern.compile("(?:từ )?tháng trước (?:tới|đến|cho tới) (?:tháng này|giờ|nay|hôm nay)");
  private static final Pattern RANGE_ONLY =
      Pattern.compile(
          "(?:em xem |xem |cho anh xem |cho mình xem )?(?:từ tháng trước (?:tới|đến) (?:tháng"
              + " này|giờ|nay|hôm nay)|tháng này|tháng trước|hôm nay|hôm qua|từ hôm qua tới giờ)"
              + POLITE_ENDING);
  private static final Pattern FULL_QUERY =
      Pattern.compile(
          "(?:anh muốn |mình muốn |tôi muốn )?(?:xem |hết )?(?:tất cả |các )?"
              + "(?:khoản chi|chi tiêu|chi|khoản thu|thu nhập|thu) "
              + "(?:từ tháng trước (?:tới|đến|cho tới) (?:tháng này|giờ|nay|hôm nay)"
              + "|tháng này|tháng trước|hôm nay|hôm qua|từ hôm qua tới giờ|từ đầu tháng tới giờ)"
              + POLITE_ENDING);
  private static final Pattern ACK =
      Pattern.compile(
          "(?:ok|oke|ừ|ừm|uh|cảm ơn|cám ơn|thanks|vâng|dạ|được|rồi|đúng rồi)"
              + "(?: em| bạn| anh| chị| rất| nhiều| lắm| nhé| nha| ạ| rồi)*[.! ]*");
  private static final String DATE_LITERAL = "[0-9]{1,4}[-/][0-9]{1,2}(?:[-/][0-9]{1,4})?";
  private static final Pattern DATE_CUE =
      Pattern.compile("\\bngày " + DATE_LITERAL + "(?![0-9])", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern DATE_ONLY =
      Pattern.compile(
          "(?:ngày "
              + DATE_LITERAL
              + "|[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}|[0-9]{1,2}/[0-9]{1,2}/[0-9]{4})"
              + POLITE_ENDING);
  private static final Pattern AMBIGUOUS_NUMBER =
      Pattern.compile(
          "(?:(?:thêm|còn|tiếp) )?[0-9]+(?:[.,][0-9]+)*(?:/[0-9]+(?:[.,][0-9]+)*)*"
              + "(?:\\s*(?:k|tr|triệu|nghìn|ngàn|đ|đồng|vnd))?(?: nữa| hôm nay| hôm qua)?"
              + POLITE_ENDING);
  private static final Pattern GREETING =
      Pattern.compile("(?:xin chào|chào em|hello)(?: userid=[0-9]+)?[.!?]*");

  public record Intent(boolean viewing, TransactionType type, TransactionQuery query) {}

  private static String normalized(String text) {
    return text.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  public static Intent interpret(String text, TransactionType previousType) {
    var value = normalized(text);
    boolean expense = value.matches(".*\\bchi\\b.*");
    boolean income = value.matches(".*\\bthu\\b.*");
    boolean rangeOnly = RANGE_ONLY.matcher(value).matches();
    boolean viewing =
        LEDGER_VIEW_REQUEST.matcher(value).matches()
            || (value.startsWith("xem ") && rangeOnly)
            || (value.startsWith("anh muốn hết tất cả khoản ") && RANGE.matcher(value).find());
    boolean followup = previousType != null && rangeOnly;
    // A new financial directive containing a range must never be mistaken for a follow-up.
    if (!viewing && !followup) {
      return new Intent(false, null, null);
    }
    TransactionType type =
        expense == income ? null : expense ? TransactionType.EXPENSE : TransactionType.INCOME;
    if (followup) {
      type = previousType;
    }
    QueryPeriod period = null;
    if (RANGE.matcher(value).find()) period = QueryPeriod.SINCE_LAST_MONTH;
    else if (value.contains("tháng này") || value.contains("từ đầu tháng tới giờ"))
      period = QueryPeriod.THIS_MONTH;
    else if (value.contains("tháng trước")) period = QueryPeriod.LAST_MONTH;
    else if (value.contains("từ hôm qua tới giờ")) period = QueryPeriod.SINCE_YESTERDAY;
    else if (value.contains("hôm qua")) period = QueryPeriod.YESTERDAY;
    else if (value.contains("hôm nay")) period = QueryPeriod.TODAY;
    // Only recover these unambiguous phrases; explicit dates and other language stay with the
    // model.
    var query =
        type == null || period == null || (!followup && !FULL_QUERY.matcher(value).matches())
            ? null
            : new TransactionQuery(type, period, null, null);
    return new Intent(true, type, query);
  }

  public static boolean canCreate(String text) {
    var value = normalized(text);
    return canAnswerAmount(value) && !AMBIGUOUS_NUMBER.matcher(value).matches();
  }

  public static boolean canAnswerAmount(String text) {
    var value = normalized(text);
    // Deny known context-only answers; other wording keeps the baseline model extraction boundary.
    return !ACK.matcher(value).matches()
        && !RANGE_ONLY.matcher(value).matches()
        && !DATE_ONLY.matcher(value).matches()
        && !GREETING.matcher(value).matches();
  }

  public static boolean hasDateCue(String text) {
    return DATE_CUE.matcher(normalized(text)).find();
  }

  public static boolean acknowledgment(String text) {
    return ACK.matcher(normalized(text)).matches();
  }

  private VietnameseQueryRecovery() {}
}
