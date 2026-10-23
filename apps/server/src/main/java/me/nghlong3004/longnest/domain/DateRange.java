package me.nghlong3004.longnest.domain;

import java.time.DateTimeException;
import java.time.LocalDate;

/** Inclusive financial-date range; independent of creation instants and host timezone. */
public record DateRange(LocalDate from, LocalDate to) {
  public DateRange {
    if (from == null || to == null || from.getYear() < 1900 || to.getYear() > 9999
        || from.isAfter(to)) {
      throw new IllegalArgumentException("Invalid financial date range");
    }
  }

  public static DateRange parse(String from, String to) {
    try {
      if (from == null || to == null || !from.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
          || !to.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
        throw new IllegalArgumentException("Invalid financial date range");
      }
      return new DateRange(LocalDate.parse(from), LocalDate.parse(to));
    } catch (DateTimeException invalid) {
      throw new IllegalArgumentException("Invalid financial date range", invalid);
    }
  }
}
