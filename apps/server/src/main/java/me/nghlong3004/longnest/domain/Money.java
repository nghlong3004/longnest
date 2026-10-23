package me.nghlong3004.longnest.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/** A positive whole-dong VND transaction magnitude, never a signed balance or total. */
public record Money(long amountMinor) {
  public static final long MAX_AMOUNT = 1_000_000_000_000L;
  public static final String CURRENCY = "VND";

  private static final Pattern INPUT =
      Pattern.compile(
          "([0-9]+(?:[.,][0-9]{1,2})?)(k|tr)|([0-9]+)(?: ?(?:đ|vnd))?",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  public Money {
    if (amountMinor < 1 || amountMinor > MAX_AMOUNT) {
      throw new IllegalArgumentException("VND transaction magnitude must be within 1..10^12");
    }
  }

  /** Parses the finite unsigned amount grammar without rounding or rescaling guesses. */
  public static Money parse(String input) {
    var match = INPUT.matcher(Objects.requireNonNull(input, "input"));
    if (!match.matches()) {
      throw new IllegalArgumentException("Unsupported VND amount syntax");
    }
    String number = match.group(3);
    int scale = 0;
    if (number == null) {
      number = match.group(1).replace(',', '.');
      scale = match.group(2).equalsIgnoreCase("k") ? 3 : 6;
    }
    try {
      return new Money(new BigDecimal(number).movePointRight(scale).longValueExact());
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "VND magnitude is not an exact supported integer", exception);
    }
  }

  public String canonicalAmount() {
    return Long.toString(amountMinor);
  }
}
