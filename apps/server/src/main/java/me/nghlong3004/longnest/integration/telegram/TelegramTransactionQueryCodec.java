package me.nghlong3004.longnest.integration.telegram;

import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import me.nghlong3004.longnest.model.dto.TransactionCursor;
import me.nghlong3004.longnest.model.dto.TransactionPage;
import me.nghlong3004.longnest.model.dto.TransactionPageRequest;
import me.nghlong3004.longnest.model.dto.TransactionQuery;
import me.nghlong3004.longnest.model.enums.QueryPeriod;
import me.nghlong3004.longnest.model.enums.TransactionType;
import org.springframework.stereotype.Component;

/** Stateless pagination token; contains no owner or authoritative financial data. */
@Component
public class TelegramTransactionQueryCodec {

  static final String PREFIX = "q:";
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

  public String encode(TransactionPage page, int messageId) {
    var cursor = page.nextCursor();
    if (cursor == null || messageId <= 0) {
      throw new IllegalArgumentException("A next cursor and original message ID are required");
    }
    var bytes =
        ByteBuffer.allocate(16)
            .putLong(cursor.id().getMostSignificantBits())
            .putLong(cursor.id().getLeastSignificantBits())
            .array();
    var id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var token =
        PREFIX
            + (page.filter().type() == TransactionType.EXPENSE ? "E" : "I")
            + ":"
            + DATE.format(page.filter().from())
            + ":"
            + DATE.format(page.filter().to())
            + ":"
            + DATE.format(cursor.date())
            + ":"
            + id
            + ":"
            + messageId;
    if (token.length() > 64) {
      throw new IllegalArgumentException("Pagination token exceeds Telegram callback limit");
    }
    return token;
  }

  public TransactionPageRequest decode(String token) {
    if (token == null || token.length() > 64 || !token.startsWith(PREFIX)) {
      throw new IllegalArgumentException("Invalid pagination token");
    }
    var parts = token.split(":", -1);
    if (parts.length != 7
        || !parts[5].matches("[A-Za-z0-9_-]{22}")
        || !parts[6].matches("[1-9][0-9]{0,9}")) {
      throw new IllegalArgumentException("Invalid pagination token");
    }
    var type =
        switch (parts[1]) {
          case "E" -> TransactionType.EXPENSE;
          case "I" -> TransactionType.INCOME;
          default -> throw new IllegalArgumentException("Invalid transaction type");
        };
    var from = date(parts[2]);
    var to = date(parts[3]);
    var cursorDate = date(parts[4]);
    var bytes = ByteBuffer.wrap(Base64.getUrlDecoder().decode(parts[5]));
    var id = new UUID(bytes.getLong(), bytes.getLong());
    int messageId = Integer.parseInt(parts[6]);
    if (from.isAfter(to) || cursorDate.isBefore(from) || cursorDate.isAfter(to)) {
      throw new IllegalArgumentException("Invalid pagination dates");
    }
    return new TransactionPageRequest(
        new TransactionQuery(type, QueryPeriod.CUSTOM, from, to),
        new TransactionCursor(cursorDate, id),
        messageId);
  }

  private LocalDate date(String value) {
    if (!value.matches("[0-9]{8}") || value.startsWith("0000")) {
      throw new IllegalArgumentException("Invalid pagination date");
    }
    try {
      return LocalDate.parse(value, DATE);
    } catch (DateTimeException exception) {
      throw new IllegalArgumentException("Invalid pagination date", exception);
    }
  }
}
