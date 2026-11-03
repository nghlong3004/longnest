package me.nghlong3004.longnest.model.enums;

/** Calendar periods resolved against the incoming message date in the ledger time zone. */
public enum QueryPeriod {
  TODAY,
  YESTERDAY,
  SINCE_YESTERDAY,
  THIS_MONTH,
  LAST_MONTH,
  SINCE_LAST_MONTH,
  SINCE_DATE,
  CUSTOM
}
